package joselusc.libraries.file2file.gui;

import com.formdev.flatlaf.FlatLightLaf;
import joselusc.libraries.file2file.converters.factory.ConverterFactory;
import joselusc.libraries.file2file.converters.interfaces.Converter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.dnd.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;

public class File2FileGUI extends JFrame {

    private JTextField fileField;
    private Color fileFieldBg;
    private JComboBox<String> converterCombo;
    private JButton browseButton, convertButton;

    public File2FileGUI() {
        super("File2File Converter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(520, 240);
        setLocationRelativeTo(null);
        setResizable(false);

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {}

        initUI();
    }

    private void initUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 30, 20, 30));
        content.setBackground(Color.WHITE);

        JLabel title = new JLabel("File2File Converter");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        content.add(Box.createVerticalStrut(20));

        converterCombo = new JComboBox<>(new String[]{"CSH to SH", "Encoding"});
        converterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        converterCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        content.add(new LabeledPanel("Converter", converterCombo));

        fileField = new JTextField();
        fileField.setEditable(false);
        fileField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fileFieldBg = fileField.getBackground();

        new DropTarget(fileField, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (isSingleFileDrag(dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    fileField.setBackground(new Color(220, 235, 250));
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                fileField.setBackground(fileFieldBg);
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                fileField.setBackground(fileFieldBg);
                if (isSingleFileDrag(dtde)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    try {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        File f = files.get(0);
                        if (f.isFile()) {
                            fileField.setText(f.getAbsolutePath());
                            dtde.dropComplete(true);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                dtde.rejectDrop();
                dtde.dropComplete(false);
            }

            private boolean isSingleFileDrag(DropTargetDragEvent dtde) {
                if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false;
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    return files.size() == 1 && files.get(0).isFile();
                } catch (Exception e) {
                    return false;
                }
            }

            private boolean isSingleFileDrag(DropTargetDropEvent dtde) {
                if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false;
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    return files.size() == 1 && files.get(0).isFile();
                } catch (Exception e) {
                    return false;
                }
            }
        });
        browseButton = new JButton("...");
        browseButton.addActionListener(this::onBrowse);

        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);
        filePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        content.add(new LabeledPanel("File", filePanel));

        content.add(Box.createVerticalStrut(20));

        convertButton = new JButton("Convert");
        convertButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        convertButton.setBackground(new Color(0, 120, 215));
        convertButton.setForeground(Color.WHITE);
        convertButton.setFocusPainted(false);
        convertButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        convertButton.addActionListener(this::onConvert);
        content.add(convertButton);

        setContentPane(content);
    }

    private void onBrowse(ActionEvent e) {
        class FilteringFileSystemView extends FileSystemView {
            private final FileSystemView delegate = FileSystemView.getFileSystemView();
            private String filterText = "";

            public void setFilterText(String text) {
                filterText = text == null ? "" : text.toLowerCase();
            }

            @Override
            public File createNewFolder(File containingDir) throws java.io.IOException {
                return delegate.createNewFolder(containingDir);
            }

            @Override
            public File[] getRoots() {
                return delegate.getRoots();
            }

            @Override
            public File getHomeDirectory() {
                return delegate.getHomeDirectory();
            }

            @Override
            public File getDefaultDirectory() {
                return delegate.getDefaultDirectory();
            }

            @Override
            public File createFileObject(File dir, String filename) {
                return delegate.createFileObject(dir, filename);
            }

            @Override
            public File createFileObject(String path) {
                return delegate.createFileObject(path);
            }

            @Override
            public File[] getFiles(File dir, boolean useFileHiding) {
                File[] files = delegate.getFiles(dir, useFileHiding);
                if (filterText.isEmpty()) {
                    return files;
                }
                java.util.List<File> filtered = new java.util.ArrayList<>();
                for (File f : files) {
                    if (f.getName().toLowerCase().contains(filterText)) {
                        filtered.add(f);
                    }
                }
                return filtered.toArray(new File[0]);
            }

            @Override
            public File getParentDirectory(File dir) {
                return delegate.getParentDirectory(dir);
            }

            @Override
            public File[] getChooserComboBoxFiles() {
                return delegate.getChooserComboBoxFiles();
            }

            @Override
            public boolean isFileSystemRoot(File f) {
                return delegate.isFileSystemRoot(f);
            }

            @Override
            public boolean isDrive(File dir) {
                return delegate.isDrive(dir);
            }

            @Override
            public boolean isFloppyDrive(File dir) {
                return delegate.isFloppyDrive(dir);
            }

            @Override
            public boolean isComputerNode(File dir) {
                return delegate.isComputerNode(dir);
            }

            @Override
            public boolean isFileSystem(File f) {
                return delegate.isFileSystem(f);
            }

            @Override
            public boolean isHiddenFile(File f) {
                return delegate.isHiddenFile(f);
            }

            @Override
            public boolean isRoot(File f) {
                return delegate.isRoot(f);
            }

            @Override
            public Boolean isTraversable(File f) {
                return delegate.isTraversable(f);
            }

            @Override
            public String getSystemDisplayName(File f) {
                return delegate.getSystemDisplayName(f);
            }

            @Override
            public String getSystemTypeDescription(File f) {
                return delegate.getSystemTypeDescription(f);
            }

            @Override
            public Icon getSystemIcon(File f) {
                return delegate.getSystemIcon(f);
            }

            @Override
            public Icon getSystemIcon(File f, int width, int height) {
                return delegate.getSystemIcon(f, width, height);
            }

            @Override
            public boolean isParent(File folder, File file) {
                return delegate.isParent(folder, file);
            }

            @Override
            public File getChild(File parent, String fileName) {
                return delegate.getChild(parent, fileName);
            }

            @Override
            public boolean isLink(File f) {
                return delegate.isLink(f);
            }

            @Override
            public File getLinkLocation(File f) throws java.io.FileNotFoundException {
                return delegate.getLinkLocation(f);
            }
        }

        FilteringFileSystemView fsv = new FilteringFileSystemView();

        JFileChooser chooser = new JFileChooser(fsv) {
            @Override
            public void approveSelection() {
                File selected = getSelectedFile();
                if (selected != null && selected.isFile()) {
                    fileField.setText(selected.getAbsolutePath());
                }
                super.approveSelection();
            }
        };

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);

        JPanel accessory = new JPanel(new BorderLayout(5, 5));
        accessory.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        accessory.setBackground(Color.WHITE);

        JLabel filterLabel = new JLabel("Filter:");
        JTextField filterField = new JTextField();

        filterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        filterField.setBackground(new Color(245, 245, 245));
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        accessory.add(filterLabel, BorderLayout.NORTH);
        accessory.add(filterField, BorderLayout.CENTER);
        chooser.setAccessory(accessory);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }

            private void update() {
                fsv.setFilterText(filterField.getText());
                chooser.rescanCurrentDirectory();
            }
        });

        chooser.showOpenDialog(this);
    }
    
    private void onConvert(ActionEvent e) {
        String filePath = fileField.getText();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedConverter = (String) converterCombo.getSelectedItem();
        String targetType = selectedConverter.equals("CSH to SH") ? "sh" : "encoding";

        try {
            Converter converter = ConverterFactory.getConverter(filePath, targetType);
            if (converter == null) {
                JOptionPane.showMessageDialog(this, "No converter found for this type.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            File output = converter.convert(filePath);
            File renamed = new File(getConvertedFileName(filePath, targetType));
            if (output.renameTo(renamed)) {
                JOptionPane.showMessageDialog(this, "Conversion successful:\n" + renamed.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Conversion done, but could not rename the file.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getConvertedFileName(String originalPath, String targetType) {
        File original = new File(originalPath);
        String name = original.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        String ext = targetType.equals("sh") ? ".sh" : ".txt";
        return new File(original.getParent(), base + "_CONVERTED" + ext).getAbsolutePath();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new File2FileGUI().setVisible(true));
    }

    static class LabeledPanel extends JPanel {
        public LabeledPanel(String labelText, JComponent field) {
            setLayout(new BorderLayout(5, 5));
            setBackground(Color.WHITE);
            JLabel label = new JLabel(labelText);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            add(label, BorderLayout.NORTH);
            add(field, BorderLayout.CENTER);
        }
    }
}