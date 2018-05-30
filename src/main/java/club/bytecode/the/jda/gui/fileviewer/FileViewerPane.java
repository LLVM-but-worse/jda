package club.bytecode.the.jda.gui.fileviewer;

import club.bytecode.the.jda.FileChangeNotifier;
import club.bytecode.the.jda.JDA;
import club.bytecode.the.jda.Resources;
import club.bytecode.the.jda.gui.JDAWindow;
import club.bytecode.the.jda.gui.MainViewerGUI;
import club.bytecode.the.jda.gui.dialogs.TabbedPane;
import club.bytecode.the.jda.gui.navigation.FileNavigationPane;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The pane that contains all of the classes as tabs.
 *
 * @author Konloch
 * @author WaterWolf
 */

public class FileViewerPane extends JDAWindow {

    private static final long serialVersionUID = 6542337997679487946L;

    FileChangeNotifier fcn;
    public JTabbedPane tabs;

    JPanel buttonPanel;
    public JButton refreshClass;

    // todo: once we move to mapleir, we can convert this to an indexedlist!
    List<ViewerFile> workingOn = new ArrayList<>();

    public FileViewerPane(final FileChangeNotifier fcn) {
        super("WorkPanel", "Work Space", Resources.fileNavigatorIcon, (MainViewerGUI) fcn);

        this.tabs = new JTabbedPane();
        this.fcn = fcn;

        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(tabs, BorderLayout.CENTER);

        buttonPanel = new JPanel(new FlowLayout());

        refreshClass = new JButton("Refresh");
        refreshClass.addActionListener(e -> (new Thread(() -> {
            final Component tabComp = tabs.getSelectedComponent();
            if (tabComp != null) {
                assert(tabComp instanceof Viewer);
                Viewer viewer = (Viewer) tabComp;
                JDA.viewer.setIcon(true);
                viewer.refresh(refreshClass);
                JDA.viewer.setIcon(false);
            }
        })).start());

        buttonPanel.add(refreshClass);

        buttonPanel.setVisible(false);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        tabs.addContainerListener(new ContainerListener() {

            @Override
            public void componentAdded(final ContainerEvent e) {
            }

            @Override
            public void componentRemoved(final ContainerEvent e) {
                final Component c = e.getChild();
                if (c instanceof Viewer) {
                    Viewer v = (Viewer) c;
                    workingOn.remove(v.getFile());
                }
            }

        });
        tabs.addChangeListener(arg0 -> buttonPanel.setVisible(tabs.getSelectedIndex() != -1));

        this.setVisible(true);

    }
    
    public static Dimension defaultDimension = new Dimension(-FileNavigationPane.defaultDimension.width, -35);
    public static Point defaultPosition = new Point(FileNavigationPane.defaultDimension.width, 0);

    @Override
    public Dimension getDefaultSize() {
        return defaultDimension;
    }

    @Override
    public Point getDefaultPosition() {
        return defaultPosition;
    }

    private void addFile(ViewerFile file, Supplier<Viewer> viewerFactory) {
        if (!workingOn.contains(file)) {
            final JPanel tabComp = viewerFactory.get();
            tabs.add(tabComp);
            final int tabCount = tabs.indexOfComponent(tabComp);
            workingOn.add(tabCount, file);
            tabs.setTabComponentAt(tabCount, new TabbedPane(file.name, tabs));
            tabs.setSelectedIndex(tabCount);
        } else {
            tabs.setSelectedIndex(workingOn.indexOf(file));
        }
    }
    
    public void addWorkingFile(ViewerFile file, final ClassNode cn) {
        addFile(file, () -> new ClassViewer(file, cn));
    }

    public void addFile(ViewerFile file, byte[] contents) {
        addFile(file, () -> new FileViewer(file, contents));
    }

    @Override
    public void openClassFile(ViewerFile file, final ClassNode cn) {
        addWorkingFile(file, cn);
    }

    @Override
    public void openFile(ViewerFile file, byte[] content) {
        addFile(file, content);
    }

    public Viewer getCurrentViewer() {
        return (Viewer) tabs.getSelectedComponent();
    }
    
    public Viewer[] getLoadedViewers() {
        return (Viewer[]) tabs.getComponents();
    }

    /**
     * @return a copy of the files currently open
     */
    public List<ViewerFile> getOpenFiles() {
        return new ArrayList<>(workingOn);
    }
    
    public void resetWorkspace() {
        for (Component component : tabs.getComponents()) {
            if (component instanceof ClassViewer)
                ((ClassViewer) component).reset();
        }
        tabs.removeAll();
        tabs.updateUI();
    }

}