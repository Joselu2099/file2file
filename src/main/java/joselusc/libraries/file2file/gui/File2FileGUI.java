package joselusc.libraries.file2file.gui;

import com.formdev.flatlaf.FlatLightLaf;
import joselusc.libraries.file2file.converters.factory.ConverterFactory;
import joselusc.libraries.file2file.converters.interfaces.Converter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class File2FileGUI extends JFrame {

    private JTextField fileField;
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
        JFileChooser chooser = new JFileChooser() {
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
        chooser.setAcceptAllFileFilterUsed(false);
        File initialDir = chooser.getCurrentDirectory();

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

        // Filtro personalizado con estado
        class DynamicFilter extends FileFilter {
            private String filterText = "";

            public void setFilterText(String text) {
                this.filterText = text.toLowerCase();
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().contains(filterText);
            }

            @Override
            public String getDescription() {
                return "Filtered files";
            }
        }

        DynamicFilter dynamicFilter = new DynamicFilter();
        chooser.setFileFilter(dynamicFilter);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }

            private void update() {
                dynamicFilter.setFilterText(filterField.getText());
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