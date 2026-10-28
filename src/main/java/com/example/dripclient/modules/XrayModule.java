package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;

/** Zaslepka: Xray jest wylaczony w tej wersji (nie jest rejestrowany w menu). */
public class XrayModule extends Module {
    public XrayModule() {
        super("Xray", Category.RENDER);
    }
}
