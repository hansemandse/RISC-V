/*
 * RISC-V Instruction Set Simulator
 * 
 * A simple single-cycle simulator of the RV32I instruction set architecture.
 * 
 * @author Hans Jakob Damsgaard (hansjakobdamsgaard@gmail.com)
 */

import java.io.*;
import java.util.*;

public class IsaSim {
	// Insert path to binary file containing RISC-V instructions
	public final static String FILEPATH = "tests/task2/branchmany.bin";

	// Initial value of the program counter (default is zero)
	public final static Integer INITIAL_PC = 0;

	// Initial value of the stack pointer (depends on wanted memory size (currently 64 KB))
	public final static Integer INITIAL_SP = 2^16;

	// Activate/deactivate debugging prints
	public final static Boolean DEBUGGING = true;

	// Static variables used throughout the simulator
	static int pc = INITIAL_PC; // Program counter (counting in bytes)
	static int sp = INITIAL_SP; // Stack pointer (counting in bytes)
	static int reg[] = new int[32]; // 32 registers

	// A single memory for instructions and data allowing "byte addressing"
	// using different integer key values (pc and sp counting in bytes)
	public static Map<Integer, Integer> progr = new HashMap<Integer, Integer>();

	public static void main(String[] args) throws IOException {
		System.out.println("Hello RISC-V World!");
		readBinary(FILEPATH); // Read instructions into memory
		boolean offsetPC = false;

		for (;;) {
			// Combine four bytes to produce a single instruction
			int instr = progr.get(pc);
			// Retrieve the least significant seven bits of the instruction
			// indicating the type of instruction
			int opcode = instr & 0x7f;
			// Execute the instruction based on its type
			switch (opcode) {
				case 0x37: // LUI
					if (DEBUGGING) {System.out.println("LUI instruction");}
					loadUpperImmediate(instr);
					break;

				case 0x17: // AUIPC
					if (DEBUGGING) {System.out.println("AUIPC instruction");}
					addUpperImmediatePC(instr);
					break;

				case 0x6F: // JAL
					jumpAndLink(instr);
					offsetPC = true;
					break;

				case 0x67: // JALR
					jumpAndLinkRegister(instr);
					offsetPC = true;
					break;

				case 0x63: // Branch instructions
					if (DEBUGGING) {System.out.println("Branch instruction");}
					offsetPC = branchInstruction(instr);
					break;

				case 0x03: // Load instructions
					break;

				case 0x23: // Store instructions
					break;

				case 0x13: // Immediate instructions
					if (DEBUGGING) {System.out.println("Immediate instruction");}
					immediateInstruction(instr);
					break;

				case 0x33: // Arithmetic instructions
					if (DEBUGGING) {System.out.println("Arithmetic instruction");}
					arithmeticInstruction(instr);
					break;

				case 0x0F: // Fence (NOT IMPLEMENTED)
					break;
				case 0x73: // Ecalls and CSR (SOME IMPLEMENTED, SOME LEFT OUT)
					break;
				default:
					if (DEBUGGING) {System.out.println("Opcode " + opcode + " not yet implemented");}
					break;
			}

			if (!offsetPC) {
				pc += 4; // Update program counter
			}
			offsetPC = false; // Reset the pc offset flag
			reg[0] = 0; // Resetting the x0 register

			// No entry in the memory for the updated pc means execution has finished
			if (progr.get(pc) == null) { 
				printFile(FILEPATH);
				break;
			}
		}
		System.out.println("Program exit");
	}

	public static void loadUpperImmediate(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int imm = instr & 0xFFFFF000;
		if (DEBUGGING) {System.out.println("rd = " + rd + ", imm = " + imm);}
		reg[rd] = imm; // LUI stores the immediate in the top 20 bits of register rd
	}

	public static void addUpperImmediatePC(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int imm = instr & 0xFFFFF000;
		if (DEBUGGING) {System.out.println("rd = " + rd + ", imm = " + imm);}
		reg[rd] = pc + imm;
	}

	public static void jumpAndLink(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int imm = (((instr >> 21) & 0x3FF) << 1) + (((instr >> 20) & 0x1) << 12) + (((instr >> 12) & 0xFF) << 13) + ((instr >> 31) << 22);
		if ((instr >> 31) == 1) {
			imm |= 0xFFF00000; // Sign-extension if necessary
		}
		reg[rd] = pc + 4; // JAL uses rd = x0 meaning that this result is lost
		reg[1] = pc + 4; // Store return address
		pc += imm; // Jump target address
		if (pc % 4 != 0) {
			System.out.println("Instruction fetch exception; pc not multiple of 4 bytes");
		}
	}

	public static void jumpAndLinkRegister(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int rs1 = (instr >> 15) & 0x1F;
		int imm = (instr >> 20);
		if ((imm >> 11) == 1) {
			imm |= 0xFFFFF000; // Sign-extension if necessary
		}
		reg[rd] = pc + 4; // Store return address
		pc += (reg[rs1] + imm) & 0xFFFFFFFE; // Jump target address sets LSB to 0
		if (pc % 4 != 0) {
			System.out.println("Instruction fetch exception; pc not multiple of 4 bytes");
		}
	}

	public static boolean branchInstruction(int instr) {
		// General information
		int rs1 = (instr >> 15) & 0x1F;
		int rs2 = (instr >> 20) & 0x1F;
		int imm = (((instr >> 8) & 0xF) << 1) + (((instr >> 25) & 0x3F) << 5) + (((instr >> 7) & 0x1) << 11) + ((instr >> 31) << 12);
		if ((imm >> 11) == 1) {
			imm |= 0xFFFFE000; // Sign-extension if necessary
		}
		if (DEBUGGING) {System.out.println("rs1 = " + rs1 + ", rs2 = " + rs2 + ", imm = " + imm);}
		// Determining the type of instruction
		int funct3 = (instr >> 12) & 0x7;
		switch (funct3) {
			case 0x0: // BEQ
				if (reg[rs1] == reg[rs2]) {
					pc += imm;
					return true;
				} 
				break;
			case 0x1: // BNE
				if (reg[rs1] != reg[rs2]) {
					pc += imm;
					return true;
				}
				break;
			case 0x4: // BLT
				if (reg[rs1] < reg[rs2]) {
					pc += imm;
					return true;
				}
				break;
			case 0x5: // BGE
				if (reg[rs1] >= reg[rs2]) {
					pc += imm;
					return true;
				}
				break;
			case 0x6: // BLTU
				if (((long) reg[rs1] & 0xFFFFFFFF) < ((long) reg[rs2] & 0xFFFFFFFF)) {
					pc += imm;
					return true;
				}
				break;
			case 0x7: // BGEU
				if (((long) reg[rs1] & 0xFFFFFFFF) >= ((long) reg[rs2] & 0xFFFFFFFF)) {
					pc += imm;
					return true;
				}
				break;
		}
		if (pc % 4 != 0) {
			System.out.println("Instruction fetch exception; pc not multiple of 4 bytes");
		}
		return false;
	}

	public static void immediateInstruction(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int rs1 = (instr >> 15) & 0x1F;
		int imm = (instr >> 20);
		if ((imm >> 11) == 1) {
			imm |= 0xFFFFF000; // Sign-extension if necessary
		}
		if (DEBUGGING) {System.out.println("rd = " + rd + ", rs1 = " + rs1 + ", imm = " + imm);}
		// Determining the type of instruction
		int funct3 = (instr >> 12) & 0x7;
		switch (funct3) {
			case 0x0: // ADDI
				reg[rd] = reg[rs1] + imm;
				break;
			case 0x2: // SLTI (signed comparison)
				if (reg[rs1] < imm) {
					reg[rd] = 1;
				} else {
					reg[rd] = 0;
				}
				break;
			case 0x3: // SLTIU (unsigned comparison)
				if (((long) reg[rs1] & 0xFFFFFFFF) < ((long) imm & 0xFFFFFFFF)) {
					reg[rd] = 1;
				} else {
					reg[rd] = 0;
				}
				break;
			case 0x4: // XORI
				reg[rd] = reg[rs1] ^ imm;
				break;
			case 0x6: // ORI
				reg[rd] = reg[rs1] | imm;
				break;
			case 0x7: // ANDI
				reg[rd] = reg[rs1] & imm;
				break;
			default:
				// Additional information 
				int funct7 = (instr >> 25);
				int shamt = (instr >> 20) & 0x1F;
				if (funct3 == 0x1 && funct7 == 0x00) { // SLLI
					reg[rd] = (reg[rs1] << shamt);
					break;
				} else if (funct3 == 0x20 && funct7 == 0x00) { // SRLI
					reg[rd] = (reg[rs1] >>> shamt);
					break;
				} else { // SRAI
					reg[rd] = (reg[rs1] >> shamt);
					break;
				}
		}
	}

	public static void arithmeticInstruction(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int rs1 = (instr >> 15) & 0x1F;
		int rs2 = (instr >> 20) & 0x1F;
		if (DEBUGGING) {System.out.println("rd = " + rd + ", rs1 = " + rs1 + ", rs2 = " + rs2);}
		// Determining the type of instruction
		int funct3 = (instr >> 12) & 0x7;
		int funct7 = (instr >> 25);
		switch (funct3) {
		case 0x0: // ADD or SUB
			if (funct7 == 0x00) { // ADD
				reg[rd] = reg[rs1] + reg[rs2];
			} else { // SUB
				reg[rd] = reg[rs1] - reg[rs2];
			}
			break;
		case 0x1: // SLL
			reg[rd] = (reg[rs1] << (reg[rs2] & 0x1F));
			break;
		case 0x2: // SLT (signed comparison)
			if (reg[rs1] < reg[rs2]) {
				reg[rd] = 1;
			} else {
				reg[rd] = 0;
			}
			break;
		case 0x3: // SLTU (unsigned comparison)
			if (rs1 == 0 && reg[rs2] != 0) { // See manual page 15
				reg[rd] = 1;
			} else {
				if (((long) reg[rs1] & 0xFFFFFFFF) < ((long) reg[rs2] & 0xFFFFFFFF)) {
					reg[rd] = 1;
				} else {
					reg[rd] = 0;
				}
			}
			break;
		case 0x4: // XOR
			reg[rd] = reg[rs1] ^ reg[rs2];
			break;
		case 0x5: // SRL or SRA
			if (funct7 == 0x00) { // SRL
				reg[rd] = (reg[rs1] >>> (reg[rs2] & 0x1F));
			} else { // SRA
				reg[rd] = (reg[rs1] >> (reg[rs2] & 0x1F));
			}
			break;
		case 0x6: // OR
			reg[rd] = reg[rs1] | reg[rs2];
			break;
		case 0x7: // AND
			reg[rd] = reg[rs1] & reg[rs2];
			break;
		}
	}

	public static void printFile(String filePath) throws IOException {
		PrintWriter writer = null;
		String filePathLocal = filePath.substring(0, filePath.indexOf(".")) + "_reg.txt";
		if (DEBUGGING) {System.out.println("Printing register content");}
		try {
			writer = new PrintWriter(filePathLocal, "UTF-8");
			writer.println("Post-execution register content");
			for (int i = 0; i < reg.length; i++) {
				writer.println("x" + i + " : " + reg[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
		if (DEBUGGING) {System.out.println("Finished printing register content");}
		System.out.println("Register content is in file " + filePathLocal);
	}
	
	public static void readBinary(String filePath) throws IOException, EOFException {
		FileInputStream fileStream = null;
		DataInputStream dataStream = null;
		if (DEBUGGING) {System.out.println("Reading instructions");}
		try {
			fileStream = new FileInputStream(filePath);
			dataStream = new DataInputStream(fileStream);
			int localPc = 0, instr;
			while ((instr = dataStream.readInt()) != -1) {
				progr.put(localPc, Integer.reverseBytes(instr));
				localPc += 4;
			}
		} catch (IOException e) {
			// Do nothing - the input part of this program works as it is supposed to
		} finally {
			if (fileStream != null) {
				fileStream.close();
			}
			if (dataStream != null) {
				dataStream.close();
			}
		}
		if (DEBUGGING) {System.out.println("Finished reading instructions");}
	}
}