/*
 * Memory implementation for RISC-V Instruction Set Simulator
 * 
 * A simple memory for a single-cycle RISC-V Instruction Set Simulator
 * 
 * @author Hans Jakob Damsgaard (hansjakobdamsgaard@gmail.com)
 */

import java.io.*;
import java.util.*;

public class Memory {
    // A map simulating the memory
    private Map<Integer, Byte> memory;

    // Constructor for the memory class
    public Memory() {
        this.memory = new HashMap<Integer, Byte>();
    }

    public boolean containsKey(int addr) {
        return memory.containsKey(addr);
    }

    public int readWord(int addr) {
        return ((memory.get(addr+3) << 24) & 0xFF000000) | ((memory.get(addr+2) << 16) & 0x00FF0000) |
               ((memory.get(addr+1) << 8) & 0x0000FF00) | (memory.get(addr) & 0x000000FF);
    }

    public int readHalfWord(int addr) {
        return ((memory.get(addr+1) << 8) & 0x0000FF00) | (memory.get(addr) & 0x000000FF);
    }

    public int readByte(int addr) {
        return memory.get(addr) & 0x000000FF;
    }

    public void storeWord(int addr, int value) {
        memory.put(addr, (byte) (value));
        memory.put(addr+1, (byte) (value >> 8));
        memory.put(addr+2, (byte) (value >> 16));
        memory.put(addr+3, (byte) (value >> 24));
    }

    public void storeHalfWord(int addr, int value) {
        memory.put(addr, (byte) value);
        memory.put(addr+1, (byte) (value >> 8));
    }

    public void storeByte(int addr, int value) {
        memory.put(addr, (byte) value);
    }

    public void readBinary(String filePath) throws IOException, EOFException {
		FileInputStream fileStream = null;
		DataInputStream dataStream = null;
		try {
            fileStream = new FileInputStream(filePath);
			dataStream = new DataInputStream(fileStream);
            int localPc = 0, instr;
			while ((instr = dataStream.readInt()) != -1) {
                storeWord(localPc, Integer.reverseBytes(instr));
                localPc += 4;
			}
		} catch (IOException e) {
            // Do nothing - else do e.printStackTrace();
		} finally {
			if (fileStream != null) {
				fileStream.close();
			}
			if (dataStream != null) {
				dataStream.close();
			}
		}
    }
}