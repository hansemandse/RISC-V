/*
 * Memory implementation for RISC-V Instruction Set Simulator
 * 
 * A simple memory for a single-cycle RISC-V Instruction Set Simulator
 * 
 * @author Hans Jakob Damsgaard (hansjakobdamsgaard@gmail.com)
 */

import java.io.*;
import java.util.*;

interface MemoryInterface {
    // Constructor for the Memory class
    public Memory();
    // Checks if memory contains a value at given address
    public boolean containsKey(int addr);
    // Interface methods to read different data sizes
    public int readWord(int addr);
    public int readHalfWord(int addr);
    public int readByte(int addr);
    // Interface methods to store different data sizes
    public void storeWord(int addr, int value);
    public void storeHalfWord(int addr, int value);
    public void storeByte(int addr, int value);
    // Methods for reading in binary files containing RISC-V instructions
    public void readBinary(String filePath) throws IOException, EOFException;
    public void readBinary(String filePath, int initial_pc) throws IOException, EOFException;
}

public class Memory implements MemoryInterface {
    // A map simulating the memory
    private Map<Integer, Byte> memory;

    // Constructor for the Memory class
    public Memory() {
        this.memory = new HashMap<Integer, Byte>();
    }

    public boolean containsKey(int addr) {
        return memory.containsKey(addr);
    }

    public int readWord(int addr) {
        if (memory.containsKey(addr) && memory.containsKey(addr+1) && 
            memory.containsKey(addr+2) && memory.containsKey(addr+3)) {
                return ((memory.get(addr+3) << 24) & 0xFF000000) | ((memory.get(addr+2) << 16) & 0x00FF0000) |
                       ((memory.get(addr+1) << 8) & 0x0000FF00) | (memory.get(addr) & 0x000000FF);
        } else {
            throw new NullPointerException();
        }
        
    }

    public int readHalfWord(int addr) {
        if (memory.containsKey(addr) && memory.containsKey(addr+1)) {
            return ((memory.get(addr+1) << 8) & 0x0000FF00) | (memory.get(addr) & 0x000000FF);
        } else {
            throw new NullPointerException();
        }
    }

    public int readByte(int addr) {
        if (memory.containsKey(addr)) {
            return memory.get(addr) & 0x000000FF;
        } else {
            throw new NullPointerException();
        }
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
		this.readBinary(filePath, 0);
    }

    public void readBinary(String filePath, int initial_pc) throws IOException, EOFException {
        FileInputStream fileStream = null;
		DataInputStream dataStream = null;
		try {
            fileStream = new FileInputStream(filePath);
			dataStream = new DataInputStream(fileStream);
            int localPc = initial_pc, instr;
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