# 🎉 Image Sending Feature Added!

## New Features

### ✨ What's New?
- **📸 Send Images** - Now you can send images through multicast!
- **🖼️ Image Preview** - See images directly in the chat
- **💬 Mixed Content** - Send both text messages and images
- **🔍 Full Size View** - Click any image to view it in full size
- **📊 Smart Display** - Images are automatically scaled to fit the chat window

## How to Use

### Sending Images (Sender)
1. Launch the `multicast.MulticastSender` application
2. Click **"🖼️ Select Image"** button
3. Choose an image file (JPG, PNG, or GIF)
4. Preview the image in the sender window
5. Click **"📸 Send Image"** to broadcast to all receivers
6. The image will appear in all connected receivers' chat windows!

### Sending Text (Sender)
1. Type your message in the text area
2. Click **"📤 Send Text Message"** (or press Ctrl+Enter)
3. Message appears on all receivers instantly

### Receiving (Receiver)
1. Click **"🔌 Start Listening"**
2. Both text messages and images will appear in the chat window
3. **Text messages** appear with a blue border
4. **Images** appear with a pink border and are automatically scaled
5. **Click any image** to view it in full size
6. Use **"🗑️ Clear Chat"** to clear all messages and images

## Supported Image Formats
- ✅ JPEG / JPG
- ✅ PNG
- ✅ GIF

## Technical Details

### New Files Added
- **multicast.MulticastMessage.java** - Message wrapper class that handles both TEXT and IMAGE message types

### Configuration Updates
- **Buffer Size**: Increased to 65,000 bytes to support larger images
- **Max Image Size**: 5 MB limit for images

### Image Handling
- Images are serialized using Java's serialization
- Automatically scaled to fit chat window (max 600x400)
- Original size maintained - click to view full size
- File size displayed with each image

## Example Workflow

```
1. Run multicast.MulticastSender
2. Run multicast.MulticastReceiver (multiple times for multiple users)
3. Click "Start Listening" on all receivers
4. In sender:
   - Send text: "Hello everyone!"
   - Select and send an image
   - Send more text
5. All receivers see everything in real-time!
```

## Chat Window Features

### Text Messages Display:
```
┌─────────────────────────────┐
│ 📝 Text Message #1          │
│ From: 192.168.1.100         │
│ Time: 14:30:25              │
│                             │
│ Hello everyone!             │
└─────────────────────────────┘
```

### Image Messages Display:
```
┌─────────────────────────────┐
│ 🖼️ Image Message #2         │
│ From: 192.168.1.100         │
│ Time: 14:30:30 | Size: 45 KB│
│                             │
│    [IMAGE PREVIEW]          │
│  (Click to view full size)  │
└─────────────────────────────┘
```

## Performance Notes
- Large images may take a moment to transmit
- Recommended max image size: 2-3 MB for smooth performance
- Images over 5 MB will be rejected
- UDP packet limit: ~65KB (handled automatically)

## Tips
1. **Compress large images** before sending for better performance
2. **Use JPG** for photos, **PNG** for graphics/screenshots
3. **Click images** in receiver to see full resolution
4. **Multiple receivers** can all see images simultaneously!

## What Makes This Cool?
- 📡 **One broadcast** → All receivers get the image
- 🚀 **No server needed** - direct multicast communication
- 👥 **Unlimited receivers** - add as many as you want
- 💬 **Mixed content** - text and images in same chat
- 🎨 **Beautiful UI** - color-coded messages, smooth scrolling

## Next Steps
Try it out:
```cmd
run-sender.bat
run-receiver.bat (multiple times!)
```

Send some messages and images to see multicast in action! 🎉

