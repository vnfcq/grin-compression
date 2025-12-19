package edu.grinnell.csc207.compression;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    private static final short EOF = 256;

    private static class Node implements Comparable<Node> {
        final Node left;
        final Node right;
        final int freq;
        final Short value;

        Node(short value, int freq) {
            this.left = null;
            this.right = null;
            this.freq = freq;
            this.value = value;
        }

        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.freq = left.freq + right.freq;
            this.value = null;
        }

        boolean isLeaf() {
            return value != null;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.freq, other.freq);
        }
    }

    private Node root;
    private Map<Short, String> charCodes;

    /**
     * Constructs a new HuffmanTree from a frequency map.
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Short, Integer> pair : freqs.entrySet()) {
            pq.add(new Node(pair.getKey(), pair.getValue()));
        }
        pq.add(new Node(EOF, 1));

        while (pq.size() >= 2) {
            Node left = pq.remove();
            Node right = pq.remove();
            pq.add(new Node(left, right));
        }

        this.root = pq.remove();
        this.charCodes = new HashMap<>();
        getCharCodes(this.root, "");
    }

    private void getCharCodes(Node node, String charCode) {
        if (node.isLeaf()) {
            charCodes.put(node.value, charCode);
            return;
        }
        getCharCodes(node.left, charCode + "0");
        getCharCodes(node.right, charCode + "1");
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree(BitInputStream in) {
        this.root = readNode(in);
        this.charCodes = new HashMap<>();
        getCharCodes(this.root, "");
    }

    private Node readNode(BitInputStream in) {
        int bit = in.readBit();
        if (bit == -1) {
            throw new IllegalArgumentException("Not a valid .grin file.");
        }
        if (bit == 0) {
            int charCode = in.readBits(9);
            return new Node((short) charCode, 0);
        } else {
            return new Node(readNode(in), readNode(in));
        }
    }

    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize(BitOutputStream out) {
        serializeNode(root, out);
    }

    private void serializeNode(Node node, BitOutputStream out) {
        if (node.isLeaf()) {
            out.writeBit(0);
            out.writeBits(node.value, 9);
        } else {
            out.writeBit(1);
            serializeNode(node.left, out);
            serializeNode(node.right, out);
        }
    }
   
    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode(BitInputStream in, BitOutputStream out) {
        while (true) {
            int bits = in.readBits(8);
            if (bits == -1) {
                break;
            }

            short ch = (short) bits;
            String charCode = charCodes.get(ch);
            for (char bit : charCode.toCharArray()) {
                out.writeBit(bit - '0'); // '0'->0, '1'->1
            }
        }

        String eofCode = charCodes.get(EOF);
        for (char bit : eofCode.toCharArray()) {
            out.writeBit(bit - '0'); // '0'->0, '1'->1
        }
    }
    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode(BitInputStream in, BitOutputStream out) {
        Node cur = root;

        while (true) {
            int bit = in.readBit();
            if (bit == -1) {
                return;
            }

            cur = (bit == 0) ? cur.left : cur.right;

            if (cur.isLeaf()) {
                if (cur.value == EOF) {
                    return;
                }
                out.writeBits(cur.value, 8);
                cur = root;
            }
        }
    }
}
