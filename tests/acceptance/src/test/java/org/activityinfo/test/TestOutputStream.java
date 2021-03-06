package org.activityinfo.test;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Provides a per-thread OutputStream to capture the output of tests.
 */
public class TestOutputStream extends OutputStream {

    private static PrintStream standardOutput;
    private static PrintStream standardError;

    private static ThreadLocal<OutputStream> threadOutput = new ThreadLocal<>();


    /**
     * Initializes the TestOutputStream. Must be called from the Main application thread.
     */
    public static synchronized void initialize() {

        if(standardOutput != null) {
            throw new IllegalStateException(TestOutputStream.class.getName() + " already initialized.");
        }
        
        standardOutput = System.out;
        standardError = System.err;
        
        TestOutputStream testOutputStream = new TestOutputStream();
        
        // Send the main thread output directly to the console
        threadOutput.set(standardOutput);

        PrintStream wrapper = new PrintStream(testOutputStream);
        
        System.setOut(wrapper);
        System.setErr(wrapper);
    }

    private static OutputStream getOrCreate() {
        OutputStream out = threadOutput.get();
        if(out == null) {
            out = new ByteArrayOutputStream();
            threadOutput.set(out);
        }
        return out;
    }
    
    public static String drainThreadOutput() {
        OutputStream out = getOrCreate();
        if(!(out instanceof ByteArrayOutputStream)) {
            throw new UnsupportedOperationException("Cannot reset output of main thread.");
        }
        ByteArrayOutputStream baos = (ByteArrayOutputStream) out;
        String output = new String(baos.toByteArray(), Charsets.UTF_8);
        baos.reset();
        return output;
    }
    
    @Override
    public void write(int b) throws IOException {
        getOrCreate().write(b);
    }

    @Override
    public void flush() throws IOException {
        getOrCreate().flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        getOrCreate().write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        getOrCreate().write(b, off, len);
    }

    public static PrintStream getStandardOutput() {
        Preconditions.checkState(standardOutput != null, "TestOutputStream.initialize() has not been called.");
        return standardOutput;
    }
}
