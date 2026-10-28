package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;

/** Zaslepka: modul Niebo jest wylaczony w tej wersji (nie jest rejestrowany w menu). */
public class SkyModule extends Module {
    public SkyModule() {
        super("Niebo", Category.RENDER);
    }
}
