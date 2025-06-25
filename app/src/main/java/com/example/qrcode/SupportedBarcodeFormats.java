package com.example.qrcode;

import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;
import java.util.Arrays;

import uk.org.okapibarcode.OkapiBarcode;
import uk.org.okapibarcode.backend.AustraliaPost;
import uk.org.okapibarcode.backend.AztecCode;
import uk.org.okapibarcode.backend.AztecRune;
import uk.org.okapibarcode.backend.ChannelCode;
import uk.org.okapibarcode.backend.Codabar;
import uk.org.okapibarcode.backend.CodablockF;
import uk.org.okapibarcode.backend.Code11;
import uk.org.okapibarcode.backend.Code128;
import uk.org.okapibarcode.backend.Code16k;
import uk.org.okapibarcode.backend.Code2Of5;
import uk.org.okapibarcode.backend.Code32;
import uk.org.okapibarcode.backend.Code3Of9;
import uk.org.okapibarcode.backend.Code3Of9Extended;
import uk.org.okapibarcode.backend.Code49;
import uk.org.okapibarcode.backend.Code93;
import uk.org.okapibarcode.backend.CodeOne;
import uk.org.okapibarcode.backend.Composite;
import uk.org.okapibarcode.backend.DataBar14;
import uk.org.okapibarcode.backend.DataBarExpanded;
import uk.org.okapibarcode.backend.DataBarLimited;
import uk.org.okapibarcode.backend.DataMatrix;
import uk.org.okapibarcode.backend.DpdCode;
import uk.org.okapibarcode.backend.Ean;
import uk.org.okapibarcode.backend.GridMatrix;
import uk.org.okapibarcode.backend.JapanPost;
import uk.org.okapibarcode.backend.KixCode;
import uk.org.okapibarcode.backend.KoreaPost;
import uk.org.okapibarcode.backend.Logmars;
import uk.org.okapibarcode.backend.MaxiCode;
import uk.org.okapibarcode.backend.MsiPlessey;
import uk.org.okapibarcode.backend.Pdf417;
import uk.org.okapibarcode.backend.Pharmacode;
import uk.org.okapibarcode.backend.Pharmacode2Track;
import uk.org.okapibarcode.backend.Plessey;
import uk.org.okapibarcode.backend.Postnet;
import uk.org.okapibarcode.backend.QrCode;
import uk.org.okapibarcode.backend.RoyalMail4State;
import uk.org.okapibarcode.backend.SwissQrCode;
import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.backend.Telepen;
import uk.org.okapibarcode.backend.Upc;
import uk.org.okapibarcode.backend.UpnQr;
import uk.org.okapibarcode.backend.UspsOneCode;

public class SupportedBarcodeFormats {
    private ArrayList<String> formats;
    private ArrayList<Symbol> symbols;
    private ArrayList<BarcodeFormat> barcodeFormats;

    SupportedBarcodeFormats() {
        formats = new ArrayList<>();
        symbols = new ArrayList<>();
        barcodeFormats = new ArrayList<>();

        formats.add("QR Code");
        symbols.add(new QrCode());

        formats.add("Australia Post");
        symbols.add(new AustraliaPost());

        formats.add("Aztec Code");
        symbols.add(new AztecCode());

        formats.add("Aztec Runes");
        symbols.add(new AztecRune());

        formats.add("Channel Code");
        symbols.add(new ChannelCode());

        formats.add("Codabar");
        symbols.add(new Codabar());

        formats.add("Codablock F");
        symbols.add(new CodablockF());

        formats.add("Code 11");
        symbols.add(new Code11());

        formats.add("Code 128");
        symbols.add(new Code128());

        formats.add("Code 16k");
        symbols.add(new Code16k());

        formats.add("Code 2 of 5");
        symbols.add(new Code2Of5());

        formats.add("Code 32 (Italian Pharmacode)");
        symbols.add(new Code32());

        formats.add("Code 3 of 9 (Code 39)");
        symbols.add(new Code3Of9());

        formats.add("Code 3 of 9 Extended (Code 39 Extended)");
        symbols.add(new Code3Of9Extended());

        formats.add("Code 49");
        symbols.add(new Code49());

        formats.add("Code 93");
        symbols.add(new Code93());

        formats.add("Code One");
        symbols.add(new CodeOne());

        formats.add("Data Matrix");
        symbols.add(new DataMatrix());

        formats.add("DPD Code");
        symbols.add(new DpdCode());

        formats.add("Dutch Post KIX Code");
        symbols.add(new KixCode());

        formats.add("EAN-13");
        symbols.add(new Ean());

        formats.add("EAN-8");
        symbols.add(new Ean());

        formats.add("Grid Matrix");
        symbols.add(new GridMatrix());

        formats.add("GS1 Composite");
        symbols.add(new Composite());

        formats.add("GS1 DataBar");
        symbols.add(new DataBar14());

        formats.add("GS1 DataBar Expanded");
        symbols.add(new DataBarExpanded());

        formats.add("GS1 DataBar Limited");
        symbols.add(new DataBarLimited());

        formats.add("Japan Post");
        symbols.add(new JapanPost());

        formats.add("Korea Post");
        symbols.add(new KoreaPost());

        formats.add("LOGMARS");
        symbols.add(new Logmars());

        formats.add("MaxiCode");
        symbols.add(new MaxiCode());

        formats.add("MSI");
        symbols.add(new MsiPlessey());

        formats.add("PDF417");
        symbols.add(new Pdf417());

        formats.add("Pharmacode");
        symbols.add(new Pharmacode());

        formats.add("Pharmacode Two-Track");
        symbols.add(new Pharmacode2Track());

        formats.add("Plessey");
        symbols.add(new Plessey());

        formats.add("POSTNET/PLANET");
        symbols.add(new Postnet());

        formats.add("Royal Mail 4 State");
        symbols.add(new RoyalMail4State());

        formats.add("Swiss QR Code");
        symbols.add(new SwissQrCode());

        formats.add("Telepen");
        symbols.add(new Telepen());

        formats.add("Telepen Numeric");
        symbols.add(new Telepen());

        formats.add("UPC-A");
        symbols.add(new Upc());

        formats.add("UPC-E");
        symbols.add(new Upc());

        formats.add("UPN QR");
        symbols.add(new UpnQr());

        formats.add("USPS OneCode");
        symbols.add(new UspsOneCode());
    }

    public ArrayList<String> getFormats() {
        return formats;
    }

    public Symbol getSymbol(int index) {
        return symbols.get(index);
    }

    public boolean isFormatSupported(String format) {
        return formats.contains(format);
    }
}
