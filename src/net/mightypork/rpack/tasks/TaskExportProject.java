package net.mightypork.rpack.tasks;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.mightypork.rpack.App;
import net.mightypork.rpack.Config;
import net.mightypork.rpack.gui.windows.Alerts;
import net.mightypork.rpack.hierarchy.tree.AssetTreeLeaf;
import net.mightypork.rpack.hierarchy.tree.AssetTreeNode;
import net.mightypork.rpack.hierarchy.tree.AssetTreeProcessor;
import net.mightypork.rpack.library.MagicSources;
import net.mightypork.rpack.library.Sources;
import net.mightypork.rpack.project.Project;
import net.mightypork.rpack.project.Projects;
import net.mightypork.rpack.utils.FileUtils;
import net.mightypork.rpack.utils.Log;
import net.mightypork.rpack.utils.ZipBuilder;
import net.mightypork.rpack.utils.filters.FileSuffixFilter;


public class TaskExportProject {

	public static void showDialog() {

		if (Projects.getActive() == null) return;

		initFileChooser();

		int opt = fc.showDialog(App.getFrame(), "Export");
		if (opt != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File f = fc.getSelectedFile();

		if (f.exists()) {
			//@formatter:off
			int overwrite = Alerts.askYesNoCancel(
					App.getFrame(),
					"File Exists",
					"File \"" + f.getName() + "\" already exists.\n" +
					"Do you want to overwrite it?"
			);
			//@formatter:on

			if (overwrite != JOptionPane.YES_OPTION) return;
		}

		Tasks.taskExportProject(f, new Runnable() {

			@Override
			public void run() {

				Alerts.info(App.getFrame(), "Export successful.");
			}
		});
	}


	public static void doExport(File target) throws Exception {

		Project project = Projects.getActive();

		final ZipBuilder zb = new ZipBuilder(target);

		InputStream in = null;

		File f;

		// Add includes

		if (Config.LOG_EXPORT) {
			Log.f2("Adding included extra files");
		}

		try {
			File extrasDir = project.getExtrasDirectory();

			List<File> extras = new ArrayList<File>();

			FileUtils.listDirectoryRecursive(extrasDir, null, extras);

			for (File file : extras) {

				if (!file.isFile()) return;

				String path = file.getAbsolutePath();
				path = path.replace(extrasDir.getAbsolutePath(), "");

				in = new FileInputStream(file);

				zb.addStream(path, in);

				if (Config.LOG_EXPORT) {
					Log.f3(path);
				}
			}
		} catch (Throwable t) {
			Log.e("Error when including extras.", t);
		}


		if (Config.LOG_EXPORT) {
			Log.f2("Adding project files");
		}

		// pack.png
		try {

			f = new File(project.getProjectDirectory(), "pack.png");
			if (f.exists()) {
				in = new FileInputStream(f);
			} else {
				in = FileUtils.getResource("/data/export/pack.png");
			}
			zb.addStream("pack.png", in);

		} finally {
			if (in != null) in.close();
		}


		// readme.txt
		zb.addResource("readme.txt", "/data/export/pack-readme.txt");


		// json mcmeta
		String desc = project.getProjectName();
		// escape for json
		desc = desc.replace("\\", "\\\\");
		desc = desc.replace("/", "\\/");
		desc = desc.replace("\"", "\\\"");
		String mcmeta = "{\"pack\":{\"pack_format\":1,\"description\":\"" + desc + "\"}}";
		zb.addString("pack.mcmeta", mcmeta);


		// assets
		AssetTreeProcessor processor = new AssetTreeProcessor() {

			@Override
			public void process(AssetTreeNode node) {

				if (node instanceof AssetTreeLeaf) {

					AssetTreeLeaf leaf = (AssetTreeLeaf) node;

					String logSrcAsset = "null";
					String logSrcMeta = "null";

					// file
					do {
						String srcName = leaf.resolveAssetSource();
						if (srcName == null) break;
						if (MagicSources.isVanilla(srcName)) break;
						if (MagicSources.isInherit(srcName)) break;

						InputStream data = null;

						try {

							try {
								data = Sources.getAssetStream(srcName, leaf.getAssetKey());
								if (data == null) break;

								String path = leaf.getAssetEntry().getPath();

								zb.addStream(path, data);
								logSrcAsset = srcName;
							} finally {
								if (data != null) {
									data.close();
								}
							}

						} catch (IOException e) {
							Log.e("Error getting asset stream.", e);
						}

					} while (false);

					// meta
					do {
						String srcName = node.resolveAssetMetaSource();
						if (srcName == null) break;
						if (MagicSources.isVanilla(srcName)) break;
						if (MagicSources.isInherit(srcName)) break;

						InputStream data = null;

						try {

							try {
								data = Sources.getAssetMetaStream(srcName, leaf.getAssetKey());
								if (data == null) {
									Log.w("null meta stream");
									break;
								}

								String path = leaf.getAssetEntry().getPath() + ".mcmeta";

								zb.addStream(path, data);
								logSrcMeta = srcName;
							} finally {
								if (data != null) {
									data.close();
								}
							}

						} catch (IOException e) {
							Log.e("Error getting asset meta stream.", e);
						}

					} while (false);


					if (Config.LOG_EXPORT) {
						if (!logSrcAsset.equals("null")) {
							Log.f3(leaf.getAssetKey() + "\n A: " + logSrcAsset + ", M: " + logSrcMeta + "\n");
						}
					}

				}
			}
		};


		AssetTreeNode root = App.getTreeDisplay().treeModel.getRoot();
		root.processThisAndChildren(processor);

		zb.close();
	}

	private static JFileChooser fc = null;


	private static void initFileChooser() {

		Project project = Projects.getActive();

		if (fc == null) fc = new JFileChooser();

		fc.setAcceptAllFileFilterUsed(false);
		fc.setDialogTitle("Export project");
		fc.setFileFilter(new FileFilter() {

			FileSuffixFilter fsf = new FileSuffixFilter("zip");


			@Override
			public String getDescription() {

				return "ZIP archives";
			}


			@Override
			public boolean accept(File f) {

				if (f.isDirectory()) return true;
				return fsf.accept(f);
			}
		});

		fc.setSelectedFile(null);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.setFileHidingEnabled(!Config.SHOW_HIDDEN_FILES);

		File dir = fc.getCurrentDirectory();
		File file = new File(dir, project.getDirName() + ".zip");
		fc.setSelectedFile(file);
	}
}
