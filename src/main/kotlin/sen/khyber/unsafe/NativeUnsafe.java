package sen.khyber.unsafe;

public class NativeUnsafe {
    
    private NativeUnsafe() {}
    
    private static native void registerNatives();
    
    static {
//        registerNatives();
    }
    
    static native long getAddress(Object o);
    
    static native Object getObject(long address);
    
    static native void pin(Object o);
    
    static native void unpin(Object o);
    
}
