/*
 * ServicePanel.java Copyright (C) 2024 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megan.ms.clientdialog;

import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.find.ISearcher;
import jloda.swing.message.MessageWindow;
import jloda.swing.util.ProgramProperties;
import megan.core.Director;
import megan.ms.clientdialog.commands.*;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

/**
 * A panel representing a service
 * Created by huson on 10/8/14.
 */
public class ServicePanel extends JPanel {
	private final RemoteServiceBrowser remoteServiceBrowser;
	private final IRemoteService service;

	private final JTree fileTree;

	private final Map<DefaultMutableTreeNode, String> treeNode2File;

	private final JMenuItem openMenuItem;
	private final JMenuItem compareMenuItem;
	private final JMenuItem aboutServerMenuItem;
	private final ISearcher jTreeSearcher;

	private final JMenuItem expandMenuItem;
	private final JMenuItem collapseMenuItem;

	public static String[] serviceButtonNames = {CompareSelectedFilesCommand.ALTNAME};

	/**
	 * constructor
	 */
	public ServicePanel(IRemoteService service, final RemoteServiceBrowser remoteServiceBrowser) {
		this.remoteServiceBrowser = remoteServiceBrowser;
		this.service = service;

		setBorder(BorderFactory.createTitledBorder("Location: " + service.getServerURL()));
		setLayout(new BorderLayout());

		treeNode2File = new HashMap<>();

		fileTree = createFileTree(service);
		fileTree.setCellRenderer(new MyRenderer());
		ToolTipManager.sharedInstance().registerComponent(fileTree);
		jTreeSearcher = new jloda.swing.find.JTreeSearcher(fileTree);
		fileTree.addTreeSelectionListener(e -> remoteServiceBrowser.updateView(IDirector.ENABLE_STATE));
		fileTree.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				updateFonts();
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {

			}
		});
		add(new JScrollPane(fileTree), BorderLayout.CENTER);

		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		bottom.add(Box.createHorizontalGlue());
		for (var name : serviceButtonNames) {
			final AbstractButton button = remoteServiceBrowser.getCommandManager().getButton(name);
			if (button != null)
				bottom.add(button);
		}
		bottom.add(Box.createHorizontalStrut(20));
		bottom.add(remoteServiceBrowser.getCommandManager().getButton(OpenSelectedFilesCommand.ALTNAME));
		openMenuItem = remoteServiceBrowser.getCommandManager().getJMenuItem(OpenSelectedFilesCommand.ALTNAME);
		compareMenuItem = remoteServiceBrowser.getCommandManager().getJMenuItem(CompareSelectedFilesCommand.ALTNAME);
		aboutServerMenuItem = remoteServiceBrowser.getCommandManager().getJMenuItem(ShowServerInfoCommand.NAME);
		expandMenuItem = remoteServiceBrowser.getCommandManager().getJMenuItem(ExpandNodesCommand.ALTNAME);
		collapseMenuItem = remoteServiceBrowser.getCommandManager().getJMenuItem(CollapseNodesCommand.ALTNAME);

		add(bottom, BorderLayout.SOUTH);
	}

	/**
	 * get all selected files
	 *
	 * @return list of selected files, full remote names
	 */
	public Collection<String> getSelectedFiles() {
		Set<String> set = new HashSet<>();
		TreePath[] paths = fileTree.getSelectionPaths();
		if (paths != null) {
			for (TreePath path : paths) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				if (node != null) {
					String file = treeNode2File.get(node);
					if (file != null) {
						if (service.getAvailableFiles().contains(file)) {
							file = service.getServerAndFileName(file);
							if (file != null)
								set.add(file);
						}
					}
				}
			}
		}
		return set;
	}

	public IRemoteService getService() {
		return service;
	}

	/**
	 * get all currently open remote files
	 *
	 * @return open remote files
	 */
	public Set<String> getCurrentlyOpenRemoteFiles() {
		Set<String> openFiles = new HashSet<>();
		for (IDirector iDir : ProjectManager.getProjects()) {
			Director dir = (Director) iDir;
			if (dir.getDocument().getMeganFile().isMeganServerFile()) {
				String fileName = dir.getDocument().getMeganFile().getFileName();
				openFiles.add(fileName);
			}
		}
		return openFiles;
	}

	private JTree createFileTree(final IRemoteService service) {
		final JTree tree = new JTree();
		final DefaultTreeModel treeModel = new DefaultTreeModel(null);
		tree.setModel(treeModel);

		treeNode2File.clear();

		final var path2node = new HashMap<String, DefaultMutableTreeNode>();
		final var root = new DefaultMutableTreeNode(service.getServerURL(), true);
		treeModel.setRoot(root);

		treeNode2File.put(root, ".");

		var sortedSet = new TreeSet<>(service.getAvailableFiles());
		for (var fileName : sortedSet) {
			var levels = fileName.split("/");
			var path = "";
			DefaultMutableTreeNode parent = root;
			for (var i = 0; i < levels.length; i++) {
				path += levels[i];
				DefaultMutableTreeNode node = path2node.get(path);
				if (node == null) {
					boolean isLeaf = (i == levels.length - 1);
					node = new DefaultMutableTreeNode(levels[i], !isLeaf);
					if (isLeaf) {
						treeNode2File.put(node, fileName);
						node.setUserObject("<html><b>" + node.getUserObject() + "</b></html>");
					} else
						treeNode2File.put(node, String.join("/", Arrays.copyOf(levels, i)));
					path2node.put(path, node);
				}
				parent.add(node);
				parent = node;
			}
		}
		tree.addMouseListener(new MyMouseListener());

		treeModel.reload();
		tree.validate();
		return tree;
	}

	/**
	 * open all currently selected files
	 */
	private void openSelectedFiles() {
		StringBuilder buf = new StringBuilder();

		int count = 0;
		Set<String> openFiles = getCurrentlyOpenRemoteFiles();
		for (String fileName : getSelectedFiles()) {
			if (openFiles.contains(fileName)) {
				buf.append("toFront file='").append(fileName).append("';");
			} else {
				buf.append("open file='").append(fileName).append("' readOnly=true;");
				count++;
			}
		}
		if (count > 10) {
			if (JOptionPane.showConfirmDialog(remoteServiceBrowser.getFrame(), "Do you really want to open " + count + " new files?", "Confirm", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon()) == JOptionPane.NO_OPTION)
				return;
		}
		Director dir = remoteServiceBrowser.getDir();
		dir.execute(buf.toString(), remoteServiceBrowser.getCommandManager());
	}

	public String getURL() {
		return service.getServerURL();
	}

	public ISearcher getjTreeSearcher() {
		return jTreeSearcher;
	}

	public JTree getFileTree() {
		return fileTree;
	}

	/**
	 * expand the given node
	 */

	public void expand(DefaultMutableTreeNode v) {
		if (v == null)
			v = (DefaultMutableTreeNode) fileTree.getModel().getRoot();

		for (var descendants = v.breadthFirstEnumeration(); descendants.hasMoreElements(); ) {
			v = (DefaultMutableTreeNode) descendants.nextElement();
			fileTree.expandPath(new TreePath(v.getPath()));
		}
	}

	/**
	 * expand an array of paths
	 */
	public void expand(TreePath[] paths) {
		for (TreePath path : paths) {
			expand((DefaultMutableTreeNode) path.getLastPathComponent());
		}
	}

	/**
	 * collapse the given node   or root
	 */
	public void collapse(DefaultMutableTreeNode v) {
		if (v == null)
			v = (DefaultMutableTreeNode) fileTree.getModel().getRoot();

		for (var descendants = v.depthFirstEnumeration(); descendants.hasMoreElements(); ) {
			v = (DefaultMutableTreeNode) descendants.nextElement();
			fileTree.collapsePath(new TreePath(v.getPath()));
		}
	}

	/**
	 * collapse an array of paths
	 */
	public void collapse(TreePath[] paths) {
		for (TreePath path : paths) {
			collapse((DefaultMutableTreeNode) path.getLastPathComponent());
		}
	}

	/**
	 * updates fonts used in tree
	 */
	public void updateFonts() {
		final Set<String> openFiles = getCurrentlyOpenRemoteFiles();

		for (int i = 0; i < fileTree.getRowCount(); i++) {
			DefaultMutableTreeNode v = (DefaultMutableTreeNode) fileTree.getPathForRow(i).getLastPathComponent();
			String file = treeNode2File.get(v);
			if (file != null) {
				if (openFiles.contains(service.getServerAndFileName(file))) {
					int pos = file.lastIndexOf(File.separator);
					if (pos == -1)
						v.setUserObject(file);
					else
						v.setUserObject(file.substring(pos + 1));
				} else {
					String user = v.getUserObject().toString();
					if (!user.startsWith("<html>"))
						v.setUserObject("<html><b>" + user + "</b></html>");
				}
			}
		}
	}

	public void selectAll(boolean select) {
		// todo: select only leaves
		if (select)
			fileTree.setSelectionInterval(0, fileTree.getRowCount());
		else
			fileTree.setSelectionInterval(0, 0);
	}


	class MyMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 1) {
				var path = fileTree.getPathForLocation(e.getX(), e.getY());
				if (path != null) {
					var node = (DefaultMutableTreeNode) path.getLastPathComponent();
					final String fileName = treeNode2File.get(node);
					if (fileName != null) {
						var about = service.getDescription(fileName);
						if (about != null && !about.isBlank()) {
							if (false) {
								MessageWindow.getInstance().getFrame().setVisible(true);
								MessageWindow.getInstance().getFrame().toFront();
							}
							System.err.println("\n" + node.toString().replaceAll("<.*?>", "") + ":\n" + about.trim());
							System.err.flush();
						}
					}
				}
			} else if (e.getClickCount() == 2) {
				int selRow = fileTree.getRowForLocation(e.getX(), e.getY());
				TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
				if (selRow != -1 && path != null) {
					fileTree.setSelectionRow(selRow);
				}
				remoteServiceBrowser.updateView(IDirector.ENABLE_STATE);
				if (openMenuItem.isEnabled())
					openSelectedFiles();
			}
		}

		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger()) {
				showPopupMenu(e);
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
				showPopupMenu(e);
			}
		}
	}

	private void showPopupMenu(MouseEvent e) {
		int selRow = fileTree.getRowForLocation(e.getX(), e.getY());
		TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
		if (selRow != -1 && path != null) {
			fileTree.setSelectionRow(selRow);
		}
		remoteServiceBrowser.updateView(IDirector.ENABLE_STATE);
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(openMenuItem);
		popupMenu.add(compareMenuItem);
		popupMenu.addSeparator();
		popupMenu.add(expandMenuItem);
		popupMenu.add(collapseMenuItem);
		popupMenu.addSeparator();
		popupMenu.add(aboutServerMenuItem);
		popupMenu.show(fileTree, e.getX(), e.getY());
	}

	private class MyRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			setToolTipText(null);
			try {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getPathForRow(row).getLastPathComponent();
				final String fileName = treeNode2File.get(node);
				if (fileName != null) {
					setToolTipText(service.getDescription(fileName));
				}
			} catch (Exception ignored) {
			}
			return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		}
	}
}


