# VCG UI & Social Reference

## Layout
```vcg
h(1, "Main Title")        # <h1>
h(2, "Subtitle")          # <h2>  (up to h6)

l("item 1", "item 2")     # styled bullet list

url("https://...", "text") # hyperlink
```

## Buttons & Keys
```vcg
btn("Click Me", "alert('hi')")  # interactive button
key("Ctrl+S")                    # keyboard badge
key("vcgc file.vcg")            # command badge
```

## Media
```vcg
img("photo.jpg", "alt text", "300px")
video("clip.mp4", "100%", "400px")
youtube("video_id_or_full_url")  # responsive iframe
```

## Social Media
```vcg
facebook("https://fb.com/page", "label")
instagram("@handle", "label")
xsocial("@handle", "label")
```

## Raw HTML
```vcg
html("<div style='color:red'>custom</div>")
```
