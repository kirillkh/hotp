package otp;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class Screen extends Canvas {
    static private final int FACE_MONOSPACE = Font.FACE_MONOSPACE;
    static private final int STYLE_BOLD = Font.STYLE_BOLD;
    static private final int SIZE_LARGE = Font.SIZE_LARGE;
    static private final int SIZE_MEDIUM = Font.SIZE_MEDIUM;
    
    private int width = getWidth();
    private int height = getHeight();
    private String title;
    private String content;
    protected String input = "";
    private int pinLen;
    private boolean large;

    Screen(boolean largeFont) {
	large = largeFont;
    }

    public final void update(String title, String content, int pinLen) {
        if (title == null)
            title = "";
        if (content == null)
            content = "";
        if (pinLen < 0)
            pinLen = 0;
        this.title = title;
        this.content = content;
        this.pinLen = pinLen;
        super.repaint();
    }

    public void paint(Graphics g) {
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, width, height);
	
        Font font = Font.getFont(FACE_MONOSPACE, STYLE_BOLD, large ? SIZE_LARGE : SIZE_MEDIUM);
        g.setFont(font);
        g.setColor(0);
        g.fillRect(5, height/4, width-10, height/4 - 1);
        int baseline = font.getBaselinePosition() / 2;
	
        int textWidth;
        if (this.pinLen > 0) {
            String masked = "";
            for (int i=0; i<this.pinLen; ++i) {
                masked = masked + "*";
            }
            textWidth = font.stringWidth(masked);
            this.content = masked.substring(0, this.input.length());
        } else {
            textWidth = font.stringWidth(this.content);
        }
	
        g.drawString(content, (width - textWidth)/2, 5*height/8 + baseline, 68);
        g.setColor(0xFFFFFF);
	
        font = Font.getFont(FACE_MONOSPACE, STYLE_BOLD, SIZE_LARGE);
        g.setFont(font);
        g.drawString(title, width/2, 3*height/8 + baseline, 65);
    }

    public void keyPressed(int keyCode) {
        switch (keyCode) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                if (this.pinLen <= 0)
                    return;
                this.input += (char) keyCode;
                super.repaint();
                super.serviceRepaints();
        }
    }
}