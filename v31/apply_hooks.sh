#!/bin/bash
# Inserts the calls to bttv.api.Hooks into the disassembled Twitch 31.5.2 code (run inside the builder container).
# Classes with short names (xm90, knl, ...) are obfuscated and specific to this Twitch version,
# the comments say how to find them again.
set -e
cd /usr/build/work
git checkout -q base -- .

# 1. app start: hand the mod a Context
APP=$(ls smali*/tv/twitch/android/app/consumer/TwitchApplication.smali)
perl -0pi -e 's/(\.method public final onCreate\(\)V\n    \.locals 28\n\n    \.line 1\n    move-object\/from16 v1, p0\n)/$1\n    # BTTV\n    invoke-static {v1}, Lbttv\/api\/Hooks;->onAppCreate(Landroid\/content\/Context;)V\n    # \/BTTV\n/ or die "app hook"' "$APP"

# 2. react native websocket: incoming text frames, chat is an IRC connection
#    xm90: the websocket listener of WebSocketModule, sends the "websocketMessage" event
WSL=$(ls smali*/xm90.smali)
perl -0pi -e 's/(\.method public final n\(Ljlw;Ljava\/lang\/String;\)V\n    \.locals 3\n)/$1\n    # BTTV\n    iget v0, p0, Lxm90;->f:I\n    invoke-static {v0, p2}, Lbttv\/api\/Hooks;->onWebSocketText(ILjava\/lang\/String;)Ljava\/lang\/String;\n    move-result-object p2\n    # \/BTTV\n/ or die "ws text hook"' "$WSL"

# 3. react native image source: redirect urls of our emotes
#    knl: ImageSource, created with the "uri" in the setSource method of the image view
IMG=$(ls smali*/knl.smali)
perl -0pi -e 's/(\.method public constructor <init>\(Landroid\/content\/Context;Ljava\/lang\/String;DDLrkl;\)V\n    \.locals 2\n)/$1\n    # BTTV\n    invoke-static {p2}, Lbttv\/api\/Hooks;->imageUri(Ljava\/lang\/String;)Ljava\/lang\/String;\n    move-result-object p2\n    # \/BTTV\n/ or die "image hook"' "$IMG"

# 4. react native http requests: answer emote card requests, add our emotes to the emote menu
NET=$(ls smali*/com/facebook/react/modules/network/NetworkingModule.smali)
perl -0pi -e 's/(\.method public sendRequest\(Ljava\/lang\/String;Ljava\/lang\/String;DLcom\/facebook\/react\/bridge\/ReadableArray;Lcom\/facebook\/react\/bridge\/ReadableMap;Ljava\/lang\/String;ZDZ\)V\n    \.locals 1\n)/$1\n    # BTTV\n    invoke-static\/range {p0 .. p11}, Lbttv\/api\/Hooks;->interceptHttpRequest(Ljava\/lang\/Object;Ljava\/lang\/String;Ljava\/lang\/String;DLjava\/lang\/Object;Ljava\/lang\/Object;Ljava\/lang\/String;ZDZ)Z\n    move-result v0\n    if-eqz v0, :bttv_continue\n    return-void\n    :bttv_continue\n    # \/BTTV\n/ or die "http hook"' "$NET"

# 5. react native text layout: wording of the emote card for our emotes
#    fw30: TextTransform, every text fragment is passed to it by the text layout manager (xu30)
TXT=$(ls smali*/fw30.smali)
perl -0pi -e 's/(\.method public static a\(Ljava\/lang\/String;Lgw30;\)Ljava\/lang\/String;\n    \.locals 8\n)/$1\n    # BTTV\n    invoke-static {p0}, Lbttv\/api\/Hooks;->text(Ljava\/lang\/String;)Ljava\/lang\/String;\n    move-result-object p0\n    # \/BTTV\n/ or die "text hook"' "$TXT"

# 6. wide emotes: slot in the text, source of the image view, width of the image view
#    jt30: the ReplacementSpan that reserves the slot of an inline view, created by the text layout manager (xu30)
#    the text register is reused inside the method, so the width is calculated at its start
SLOT=$(ls smali*/jt30.smali)
perl -0pi -e 's/(\.method public final getSize\(Landroid\/graphics\/Paint;Ljava\/lang\/CharSequence;IILandroid\/graphics\/Paint\$FontMetricsInt;\)I\n)    \.locals 0\n/$1    .locals 1\n\n    # BTTV\n    iget v0, p0, Ljt30;->a:I\n    invoke-static {p2, p4, v0}, Lbttv\/api\/Hooks;->inlineViewWidth(Ljava\/lang\/CharSequence;II)I\n    move-result v0\n    # \/BTTV\n/ or die "slot hook"' "$SLOT"
perl -0pi -e 's/    :cond_0\n    iget p0, p0, Ljt30;->a:I\n((?:\n|    \.line \d+\n)*)    return p0\n/    :cond_0\n$1    return v0\n/ or die "slot hook return"' "$SLOT"
#    hcw: ReactImageView, the view class of ReactImageManager
IMGVIEW=$(ls smali*/hcw.smali)
perl -0pi -e 's/(\.method public final setSource\(Lcom\/facebook\/react\/bridge\/ReadableArray;\)V\n    \.locals 20\n\n    \.line 1\n    move-object\/from16 v0, p0\n\n    \.line 2\n    \.line 3\n    move-object\/from16 v1, p1\n)/$1\n    # BTTV\n    invoke-static {v0, v1}, Lbttv\/api\/Hooks;->onImageViewSource(Ljava\/lang\/Object;Ljava\/lang\/Object;)V\n    # \/BTTV\n/ or die "image view hook"' "$IMGVIEW"
MOUNT=$(ls smali*/com/facebook/react/fabric/mounting/SurfaceMountingManager.smali)
perl -0pi -e 's/(\.method public final updateLayout\(IIIIIIII\)V\n    \.locals 6\n)/$1\n    # BTTV\n    invoke-static {p0, p1, p5, p6}, Lbttv\/api\/Hooks;->layoutWidth(Ljava\/lang\/Object;III)I\n    move-result p5\n    # \/BTTV\n/ or die "layout hook"' "$MOUNT"

# 7. chat input: TwitchEmoteInputView draws emotes as spans and downloads them itself (EmoteImageLoader),
#    every span gets its image url through the constructor of EmoteSpanTarget
INPUT=$(ls smali*/com/twitchrn/emoteinput/TwitchEmoteInputView\$EmoteSpanTarget.smali)
perl -0pi -e 's/(\.method public constructor <init>\(IIILjava\/lang\/String;Ljava\/lang\/Integer;\)V\n    \.locals 0\n)/$1\n    # BTTV\n    invoke-static {p4}, Lbttv\/api\/Hooks;->inputEmoteUrl(Ljava\/lang\/String;)Ljava\/lang\/String;\n    move-result-object p4\n    # \/BTTV\n/ or die "input emote hook"' "$INPUT"
#    images of our emotes come from our disk cache
LOADER=$(ls smali*/com/twitchrn/emoteinput/EmoteImageLoader.smali)
perl -0pi -e 's/(\.method private final fetch\(Ljava\/lang\/String;\)Landroid\/graphics\/Bitmap;\n    \.locals 2\n)/$1\n    # BTTV\n    invoke-static {p1}, Lbttv\/api\/Hooks;->inputEmoteBitmap(Ljava\/lang\/String;)Landroid\/graphics\/Bitmap;\n    move-result-object v0\n    if-eqz v0, :bttv_fetch\n    return-object v0\n    :bttv_fetch\n    # \/BTTV\n/ or die "input emote loader hook"' "$LOADER"
#    EmoteImageSpan: square of sizePx, the slot and the drawn image get the width of the emote
SPAN=$(ls smali*/com/twitchrn/emoteinput/EmoteImageSpan.smali)
perl -0pi -e 's/(\.method public getSize\(Landroid\/graphics\/Paint;Ljava\/lang\/CharSequence;IILandroid\/graphics\/Paint\$FontMetricsInt;\)I\n)    \.locals 0\n/$1    .locals 2\n\n    # BTTV\n    iget-object v0, p0, Lcom\/twitchrn\/emoteinput\/EmoteImageSpan;->url:Ljava\/lang\/String;\n    iget v1, p0, Lcom\/twitchrn\/emoteinput\/EmoteImageSpan;->sizePx:I\n    invoke-static {v0, v1}, Lbttv\/api\/Hooks;->inputEmoteWidth(Ljava\/lang\/String;I)I\n    move-result v0\n    return v0\n    # \/BTTV\n/ or die "input span size hook"' "$SPAN"
#    in draw p4 is the left edge and p3 the height, the right edge is p4 + p3 (placeholder into p6, image into p7)
for REG in p6 p7; do
perl -0pi -e 's/    add-float '$REG', p4, p3\n/    # BTTV\n    iget-object v0, p0, Lcom\/twitchrn\/emoteinput\/EmoteImageSpan;->url:Ljava\/lang\/String;\n    invoke-static {v0, p4, p3}, Lbttv\/api\/Hooks;->inputEmoteRight(Ljava\/lang\/String;FF)F\n    move-result '$REG'\n    # \/BTTV\n/ or die "input span draw hook '$REG'"' "$SPAN"
done

#    animated emotes: the span draws its still image with drawBitmap, we may draw an animation instead
perl -0pi -e 's/(    invoke-virtual \{p1, p5, p2, p3, p0\}, Landroid\/graphics\/Canvas;->drawBitmap\(Landroid\/graphics\/Bitmap;Landroid\/graphics\/Rect;Landroid\/graphics\/RectF;Landroid\/graphics\/Paint;\)V\n)/    # BTTV\n    invoke-static {p5, p1, p3, p0}, Lbttv\/api\/Hooks;->drawInputEmote(Landroid\/graphics\/Bitmap;Landroid\/graphics\/Canvas;Landroid\/graphics\/RectF;Landroid\/graphics\/Paint;)Z\n    move-result v0\n    if-eqz v0, :bttv_draw\n    return-void\n    :bttv_draw\n    # \/BTTV\n$1/ or die "input span animation hook"' "$SPAN"
#    the animations redraw the spans of the input views
VIEW=$(ls smali*/com/twitchrn/emoteinput/TwitchEmoteInputView.smali)
perl -0pi -e 's/(\.method public onAttachedToWindow\(\)V\n    \.locals 1\n)/$1\n    # BTTV\n    invoke-static {p0}, Lbttv\/api\/Hooks;->onInputViewAttached(Ljava\/lang\/Object;)V\n    # \/BTTV\n/ or die "input view hook"' "$VIEW"

git diff --stat
