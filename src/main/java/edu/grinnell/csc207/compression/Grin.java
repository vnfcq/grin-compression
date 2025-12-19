package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    private static final int MAGIC = 0x736;

    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to decode
     * @param outfile the file to ouptut to
     */
    public static void decode(String infile, String outfile) throws IOException {
        try (BitInputStream in = new BitInputStream(infile);
             BitOutputStream out = new BitOutputStream(outfile)) {

            int magic = in.readBits(32);
            if (magic != MAGIC) {
                throw new IllegalArgumentException("Not a valid .grin file.");
            }

            HuffmanTree ht = new HuffmanTree(in);
            ht.decode(in, out);
        }
    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * @param file the file to read
     * @return a freqency map for the given file
     */
    public static Map<Short, Integer> createFrequencyMap(String file) throws IOException {
        Map<Short, Integer> freqs = new java.util.HashMap<>();

        try (BitInputStream in = new BitInputStream(file)) {
            while (true) {
                int bits = in.readBits(8);
                if (bits == -1) {
                    break;
                }

                short ch = (short) bits;
                freqs.put(ch, freqs.getOrDefault(ch, 0) + 1);
            }
        }

        return freqs;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to encode.
     * @param outfile the file to write the output to.
     */
    public static void encode(String infile, String outfile) throws IOException {
        Map<Short, Integer> freqs = createFrequencyMap(infile);

        try (BitInputStream in = new BitInputStream(infile);
             BitOutputStream out = new BitOutputStream(outfile)) {
            out.writeBits(MAGIC, 32);

            HuffmanTree ht = new HuffmanTree(freqs);
            ht.serialize(out);
            ht.encode(in, out);
        }
    }

    /**
     * The entry point to the program.
     * @param args the command-line arguments.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
            return;
        }

        String mode = args[0];
        String infile = args[1];
        String outfile = args[2];

        if (mode.equals("encode")) {
            encode(infile, outfile);
        } else if (mode.equals("decode")) {
            decode(infile, outfile);
        } else {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        }
    }
}
