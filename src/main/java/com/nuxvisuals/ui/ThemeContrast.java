package com.nuxvisuals.ui;
public final class ThemeContrast {
    private ThemeContrast() {}
    private static final double[] LINEAR=new double[256];
    static {for(int n=0;n<256;n++){double c=n/255.0;LINEAR[n]=c<=0.04045?c/12.92:Math.pow((c+0.055)/1.055,2.4);}}
    private static double channel(int n) {return LINEAR[n];}
    public static double luminance(int c) {return 0.2126*channel((c>>16)&255)+0.7152*channel((c>>8)&255)+0.0722*channel(c&255);}
    public static double ratio(int a,int b) {double x=luminance(a),y=luminance(b);return (Math.max(x,y)+0.05)/(Math.min(x,y)+0.05);}
    public static int ink(int bg) {return ratio(0xFFFFFF,bg)>=ratio(0,bg)?0xFFFFFFFF:0xFF000000;}
    public static int readable(int fg,int bg) {return ratio(fg,bg)>=4.5?fg:(fg&0xFF000000)|(ink(bg)&0xFFFFFF);}
}
