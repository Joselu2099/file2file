package joselusc.libraries.file2file.gui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Simple utility panel that places a label above a component.
 * Extracted from {@link joselusc.libraries.file2file.gui.File2FileGUI}
 * to allow reuse in other dialogs.
 */
public class LabeledPanel extends JPanel {

    public LabeledPanel(String labelText, JComponent field) {
        setLayout(new BorderLayout(5, 5));
        setBackground(Color.WHITE);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        add(label, BorderLayout.NORTH);
        add(field, BorderLayout.CENTER);
    }
}
