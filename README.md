# MediaScanner 🎬🖼

A desktop application to scan your PC for images and videos, showing their name, location, type, and size.

## Requirements

- **JDK 17 or higher** — [Download here](https://adoptium.net/)
- **Maven 3.6+** — [Download here](https://maven.apache.org/download.cgi)
- **VS Code** with the **Extension Pack for Java** (by Microsoft)

## How to Run

### Option 1: VS Code
1. Open the `mediascanner` folder in VS Code
2. Open a terminal (Ctrl + `)
3. Run:
   ```
   mvn javafx:run
   ```

### Option 2: Command Line
```bash
cd mediascanner
mvn javafx:run
```

## How to Use

1. **Select a Drive** from the dropdown (e.g., `C:\`) — or click **Browse** to pick a specific folder
2. Click **⚡ Scan Now** — the app will walk through all files
3. Watch files appear in real time as they're discovered
4. Use the **search bar** to filter by name, path, or extension
5. Use **Images / Videos** toggle buttons to filter by type
6. **Double-click** any row to open its folder in Windows Explorer

## Supported Formats

**Images:** JPG, JPEG, PNG, GIF, BMP, WEBP, TIFF, ICO, SVG, HEIC, HEIF, RAW, CR2, NEF, ARW

**Videos:** MP4, AVI, MKV, MOV, WMV, FLV, WEBM, M4V, MPG, MPEG, 3GP, TS, MTS, VOB, RM

## Notes

- The scanner **skips Windows system folders** (Windows, WinSxS, System Volume Information) to avoid permission errors and speed up the scan
- Files you don't have read access to are silently skipped
- Scanning `C:\` can take several minutes depending on drive size
