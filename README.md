# wifi-password-tester-gui
WiFi Password Tester v3 is a Java-based desktop application with a user-friendly Swing GUI. It allows ethical network testers to scan nearby Wi-Fi networks and attempt to connect using passwords from a user-supplied wordlist file. Designed for educational and ethical penetration testing on authorized systems only

# 🚀 WiFi Password Tester v3 (Advanced GUI + JAR Ready)

**🔐 WiFi Password Tester v3** is a fully-featured **Java Swing GUI application** for ethical password testing on Wi-Fi networks using a provided wordlist. This version includes:

- ✅ Real-time log viewer  
- ✅ Wordlist input customization  
- ✅ Network scanner  
- ✅ One-click `.jar` launch support

> ⚠️ **Educational Tool Only** — Do not use on unauthorized networks.

---

## 🧠 Key Features

- 🔍 Scan and list nearby Wi-Fi networks (SSID, Auth, Encryption)
- ✍️ Use your own custom `wordlist.txt`
- 💬 Real-time console logs in the GUI
- 🧪 Try passwords one by one and attempt connection
- 🧹 Automatically deletes failed profiles to keep system clean
- 📦 `.jar` version included — just double-click to run!

---

## 🛠️ Requirements

- **Java JDK 8 or later**
- **Operating System**: 🪟 **Windows only** (uses `netsh` command)

---

## 📦 Files Included

```bash
.
├── src/WiFiPasswordTesterGUI.java       # Source code
├── WiFiPasswordTesterGUI.jar        # Pre-built executable JAR (double-click to run)
├── wordlist.txt                     # Sample wordlist (can be customized)
├── LICENSE                          # MIT License
└── README.md                        # You're reading it 😎
```

---

## 🚀 How to Run

### 🧑‍💻 Option 1: Using the `.jar` File

> Just double-click `WiFiPasswordTesterGUI.jar` (Java must be installed on your system).

OR

### 🧑‍💻 Option 2: Compile and Run From Source

```bash
javac WiFiPasswordTesterGUI.java
java WiFiPasswordTesterGUI
```

---

## 📦 How to Manually Create the JAR File

1. **Compile the Java source code:**

   ```bash
   javac -d . src/WiFiPasswordTesterGUI.java
   ```

2. **Create a manifest file** (e.g., `manifest.txt`) with the following contents:

   ```
   Main-Class: WiFiPasswordTesterGUI
   ```

3. **Package everything into a JAR:**

   ```bash
   jar cfm WiFiPasswordTesterGUI.jar manifest.txt WiFiPasswordTesterGUI.class
   ```

4. **Run the JAR:**

   ```bash
   java -jar WiFiPasswordTesterGUI.jar
   ```

---

## 🔧 How to Use

1. Click "Scan Networks" to search for available SSIDs.
2. Select a Wi-Fi network from the list.
3. Enter path to your password wordlist file (e.g., `rockyou.txt` or `wordlist.txt`).
4. Click "Start Testing" to begin the dictionary attack.
5. Logs will display password attempts and success/failure status.

---

## ⚠️ Legal Disclaimer

This tool is strictly for **educational purposes** and **ethical penetration testing** only.  
**Do not use** this software on networks you do not own or have explicit permission to test.

> Unauthorized access is illegal under cybercrime laws globally.

---

## 📜 License

Released under the **MIT License** — [See LICENSE](LICENSE)

---

## 👨‍💻 Author

**Muhammad Arslan** aka **CosmicHackerX**  
🌐 Passionate about ethical hacking, networking, and Java development.

---

### ✅ Bonus Tips
- Name your repo something like `wifi-password-tester-v3-gui` on GitHub.
- Place the `.jar` in a `releases/` folder or attach it to the GitHub "Releases" tab.
- Need a sample `wordlist.txt`? Include one with common passwords for demonstration.
