package com.xiaozhi003.uvc.widget;

public interface UVCPreview extends IAspectRatioView {

   void setMirror(boolean isMirror);

   boolean isMirror();

   void rotate(int angle);
}
