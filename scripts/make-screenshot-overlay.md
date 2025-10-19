# Screenshot Overlay Guide

**Author**: Aditya R.  
**Purpose**: Ensure all screenshots show current date/time and "Aditya R." name

## Required Elements in Every Screenshot

1. **System Clock**: Visible date and time (taskbar, menubar, or overlay)
2. **Author Name**: "Aditya R." visible in terminal prompt, application banner, or overlay

## Methods to Add Overlays

### Method 1: Terminal Prompt (Linux/Mac)

Add to `~/.bashrc` or `~/.zshrc`:

```bash
export PS1="\[\e[32m\]Aditya R.\[\e[m\]@\h:\w\$ "
```

Result:
```
Aditya R.@laptop:~/kaiburr-task1$
```

### Method 2: PowerShell Prompt (Windows)

Add to PowerShell profile (`$PROFILE`):

```powershell
function prompt {
    "Aditya R. @ $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') > "
}
```

Result:
```
Aditya R. @ 2025-10-18 14:30:45 >
```

### Method 3: Split Terminal with Clock

**Linux/Mac (tmux)**:

```bash
# Top pane: Application
# Bottom pane: Clock
tmux split-window -v -p 10
tmux send-keys -t 1 'watch -n 1 "echo \"Aditya R. - $(date +\"%Y-%m-%d %H:%M:%S\")\""' C-m
```

**Windows (PowerShell)**:

```powershell
# Run in separate terminal
while ($true) { 
    Clear-Host
    Write-Host "Aditya R. - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Green
    Start-Sleep -Seconds 1
}
```

### Method 4: Postman Pre-request Script

Add to Postman collection pre-request script:

```javascript
pm.environment.set("author_name", "Aditya R.");
pm.environment.set("current_timestamp", new Date().toISOString());

console.log("Author: Aditya R.");
console.log("Timestamp: " + new Date().toLocaleString());
```

Then include `{{author_name}}` in request body where appropriate.

### Method 5: Image Annotation Tool

**Linux**: Use `imagemagick` to add timestamp overlay after taking screenshot

```bash
convert screenshot.png \
    -pointsize 24 -fill white -stroke black -strokewidth 2 \
    -gravity NorthEast -annotate +10+10 "Aditya R.\n$(date +'%Y-%m-%d %H:%M:%S')" \
    screenshot_annotated.png
```

**Mac**: Use Preview → Tools → Annotate → Text

**Windows**: Use Paint or Snipping Tool annotations

### Method 6: OBS Studio Overlay (Best for Videos/GIFs)

1. Install OBS Studio
2. Add Text Source with:
   - Text: "Aditya R. | %H:%M:%S"
   - Update interval: 1000ms
3. Position in corner
4. Take screenshots or record

## Screenshot Checklist

Use this checklist when taking screenshots:

### 1. Application Startup
- [ ] Terminal shows "Aditya R." in prompt or overlay
- [ ] System clock visible (taskbar/menubar)
- [ ] Application banner shows "Aditya R." text
- [ ] Spring Boot startup logs visible
- [ ] Port 8080 shown

### 2. Postman Requests
- [ ] System clock visible
- [ ] Postman console shows "Aditya R." or timestamp in pre-request script output
- [ ] Request URL visible
- [ ] Response status code visible
- [ ] X-Correlation-Id header visible
- [ ] Response body expanded and readable

### 3. Curl Commands
- [ ] Terminal prompt shows "Aditya R."
- [ ] System clock visible (separate pane or prompt)
- [ ] Full command visible
- [ ] Response JSON formatted (use `| jq '.'`)
- [ ] Timestamp in response visible

### 4. MongoDB Data
- [ ] MongoDB Compass or mongosh visible
- [ ] System clock visible
- [ ] Author name in screenshot tool or terminal
- [ ] Task documents expanded
- [ ] Execution history visible
- [ ] Timestamps in documents visible

### 5. Validation Failure
- [ ] System clock visible
- [ ] "Aditya R." visible
- [ ] Malicious command shown in request
- [ ] 400 Bad Request status visible
- [ ] Error message with violations visible

### 6. Delete Operation
- [ ] System clock visible
- [ ] "Aditya R." visible
- [ ] DELETE request shown
- [ ] 204 No Content status visible

## Example Screenshot Workflow

### Linux/Mac Example:

```bash
# Terminal 1: Run application
cd ~/kaiburr-task1
export PS1="\[\e[32m\]Aditya R.\[\e[m\]@\h:\w\$ "
./scripts/dev-bootstrap.sh

# Terminal 2: Run demo commands (split screen)
cd ~/kaiburr-task1
export PS1="\[\e[32m\]Aditya R.\[\e[m\]@\h:\w\$ "
./scripts/demo-commands.sh

# Terminal 3: Show clock (small bottom pane)
watch -n 1 'echo "Aditya R. - $(date +%Y-%m-%d\ %H:%M:%S)"'
```

Take screenshot with system screenshot tool (ensure clock is visible in OS taskbar).

### Windows PowerShell Example:

```powershell
# Terminal 1: Run application
cd D:\Kaiburr
function prompt { "Aditya R. @ $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') > " }
mvn clean package
java -jar target\task1-1.0.0-SNAPSHOT.jar

# Terminal 2: Run curl commands
function prompt { "Aditya R. @ $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') > " }
curl http://localhost:8080/api/tasks | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

Ensure Windows taskbar with clock is visible in screenshot.

## Post-Processing Tips

1. **Crop**: Focus on relevant content, but keep clock visible
2. **Highlight**: Use colored boxes to highlight important elements
3. **Annotations**: Add arrows or text to explain what's shown
4. **Consistency**: Use same screenshot tool for all images
5. **Filenames**: Use descriptive names matching README (e.g., `01-app-startup.png`)

## Screenshot Naming Convention

```
docs/screenshots/
├── 01-app-startup.png
├── 02-create-task.png
├── 03-get-tasks.png
├── 04-get-task-by-id.png
├── 05-search-tasks.png
├── 06-execute-task.png
├── 07-validation-failure.png
├── 08-delete-task.png
└── 09-mongodb-data.png
```

## Verification Before Submission

Before submitting, verify each screenshot:

```bash
# Check all required screenshots exist
for i in {01..09}; do
    if [ ! -f "docs/screenshots/${i}-*.png" ]; then
        echo "Missing: ${i}-*.png"
    fi
done
```

Review each image:
- ✅ Date/time visible and current
- ✅ "Aditya R." visible
- ✅ Content is clear and relevant
- ✅ No sensitive information exposed
- ✅ File size reasonable (< 1 MB per image)

---

**Remember**: Consistency is key. Use the same method for all screenshots to maintain a professional appearance.
