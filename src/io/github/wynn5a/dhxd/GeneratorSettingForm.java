package io.github.wynn5a.dhxd;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;

import javax.swing.*;

/**
 * Created by wayne5a on 2016/9/3.
 */
public class GeneratorSettingForm {
    private final Logger LOG = Logger.getInstance(getClass());

    JPanel panel;
    private JButton cancelButton;
    private JTextField host;
    private JTextField port;
    private JTextField service;
    private JTextField userName;
    private JPasswordField password;
    private JPanel DetailPanel;
    private JButton generateButton;
    private JTextField tableName;
    private JTextField templateFilePath;

    public GeneratorSettingForm(Project project) {
        cancelButton.addActionListener(e -> {
            LOG.debug("clicked cancel");
            closePopup();
        });

        generateButton.addActionListener(e -> {
            GeneratorContext context = validateAndBuildGeneratorContext();
            if (context != null) {
                GenerateUtils.INSTANCE.generateDomain(project, context);
            }
        });
    }

    private void closePopup() {
        JBPopup popup = DomainGeneratorUtils.INSTANCE.getPopup();
        if (popup != null) {
            popup.dispose();
        } else {
            LOG.error("cannot get popup instance");
        }
    }

    private GeneratorContext validateAndBuildGeneratorContext() {
        String hostString = host.getText();
        if (isEmpty(hostString)) {
            showMessage("Invalid host data");
            return null;
        }

        String portString = port.getText();
        if (isEmpty(portString)) {
            showMessage("Invalid port data");
            return null;
        }

        String serviceText = service.getText();
        if (isEmpty(serviceText)) {
            showMessage("Invalid service data");
            return null;
        }

        String userNameText = userName.getText();
        if (isEmpty(userNameText)) {
            showMessage("Invalid user name data");
            return null;
        }

        String tableNameText = tableName.getText();
        if (isEmpty(tableNameText)) {
            showMessage("Invalid table name data");
            return null;
        }

        String passwordText = password.getPassword() != null ? String.valueOf(password.getPassword()) : "";
        if (isEmpty(passwordText)) {
            showMessage("Invalid password data");
            return null;
        }

        String templateFile = templateFilePath.getText();
        if (isEmpty(templateFile)) {
            showMessage("Invalid template file data");
            return null;
        }

        return new GeneratorContext(hostString.trim(), portString.trim(), serviceText.trim(), userNameText.trim(), passwordText.trim(), tableNameText.trim(), templateFile.trim());
    }

    private void showMessage(String msg) {
        JOptionPane optionPane = new JOptionPane(msg);
        JDialog dialog = optionPane.createDialog("Data should not be null or empty!");
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    private boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

}
