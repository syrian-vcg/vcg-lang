package com.syrianvcg.vcgsdk;

/**
 * Compilation result returned by VcgCompiler.
 */
public class VcgResult {
    private final boolean success;
    private final String output;
    private final String error;

    public VcgResult(boolean success, String output, String error) {
        this.success = success;
        this.output  = output;
        this.error   = error;
    }

    public boolean isSuccess()  { return success; }
    public String  getOutput()  { return output;  }
    public String  getError()   { return error;   }

    @Override
    public String toString() {
        return success ? "[OK] " + output : "[ERR] " + error;
    }
}
