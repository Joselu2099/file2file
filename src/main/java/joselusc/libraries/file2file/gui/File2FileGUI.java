package joselusc.libraries.file2file.gui;

import joselusc.libraries.file2file.converters.interfaces.Converter;
import joselusc.libraries.file2file.converters.factory.ConverterFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * GUI mejorada para ejecutar los convertidores de file2file.
 */
public class File2FileGUI extends JFrame {

    private JTextField fileField;
    private JComboBox<String> converterCombo;
    private JButton browseButton, convertButton;

    public File2FileGUI() {
        setTitle("File2File Converter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 180);
        setLocationRelativeTo(null);

        // Panel principal
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Selector de conversor
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Conversor:"), gbc);

        converterCombo = new JComboBox<>(new String[]{"CSH a SH", "Encoding"});
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(converterCombo, gbc);

        // Selector de archivo
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Archivo:"), gbc);

        fileField = new JTextField(30);
        fileField.setEditable(false);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(fileField, gbc);

        browseButton = new JButton("Browse");
        browseButton.addActionListener(this::onBrowse);
        gbc.gridx = 2; gbc.gridy = 1;
        panel.add(browseButton, gbc);

        // Botón de convertir
        convertButton = new JButton("Convertir");
        convertButton.addActionListener(this::onConvert);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(convertButton, gbc);

        setContentPane(panel);
    }

    private void onBrowse(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            fileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onConvert(ActionEvent e) {
        String filePath = fileField.getText();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un archivo.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedConverter = (String) converterCombo.getSelectedItem();
        String targetType;
        switch (selectedConverter) {
            case "CSH a SH":
                targetType = "sh";
                break;
            case "Encoding":
                targetType = "encoding";
                break;
            default:
                JOptionPane.showMessageDialog(this, "Conversor no soportado.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
        }

        try {
            Converter converter = ConverterFactory.getConverter(filePath, targetType);
            if (converter == null) {
                JOptionPane.showMessageDialog(this, "No se encontró un convertidor para ese tipo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            File output = converter.convert(filePath);

            // Renombrar el archivo de salida con _CONVERTED y extensión correspondiente
            String outputPath = getConvertedFileName(filePath, targetType);
            File renamed = new File(outputPath);
            if (output.renameTo(renamed)) {
                JOptionPane.showMessageDialog(this, "Conversión exitosa:\n" + renamed.getAbsolutePath(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Conversión realizada, pero no se pudo renombrar el archivo.", "Aviso", JOptionPane.WARNING_MESSAGE);
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
        String ext = (targetType.equals("sh")) ? ".sh" : ".txt";
        return new File(original.getParent(), base + "_CONVERTED" + ext).getAbsolutePath();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new File2FileGUI().setVisible(true));
    }
}
