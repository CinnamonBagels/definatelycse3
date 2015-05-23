package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
	public boolean isRoot;
	private LinkedList<ChildProcess> childProcesses = new LinkedList<ChildProcess>();
	
	public class ChildProcess {
		public UserProcess process;
		public int id;
		public int status;
		public UserProcess parent;
		public DescriptorManager manager;
		public ChildProcess(UserProcess child, int id, UserProcess parent) {
			this.process = child;
			this.id = id;
			this.parent = parent;
			activeProcesses++;
			manager = new DescriptorManager();
	    	manager.add(0,UserKernel.console.openForReading());
	    	manager.add(1,UserKernel.console.openForWriting());
		}
	}
    /**
     * Allocate a new process.
     */
    public UserProcess() {
    	int numPhysPages = Machine.processor().getNumPhysPages();
    	pageTable = new TranslationEntry[numPhysPages];
    	for (int i=0; i<numPhysPages; i++)
    		pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
    	manger = new DescriptorManager();
    	manger.add(0,UserKernel.console.openForReading());
    	manger.add(1,UserKernel.console.openForWriting());
    	id = processID++;
    	activeProcesses++;
    	activeProcess.add(id);
    	
    	
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
//	if (vaddr < 0 || vaddr >= memory.length)
//	    return 0;

	
	
	int numbyteTransferred = 0;
	while(numbyteTransferred < length){
		int virtualpagenumber = Processor.pageFromAddress(vaddr + numbyteTransferred);
		if(virtualpagenumber < 0 || virtualpagenumber >= pageTable.length){
			return 0;
		}
//		if(pageTable[virtualpagenumber] != null){
//			return -1;
//		}
		
		int virtualpageoffset = Processor.offsetFromAddress(vaddr + numbyteTransferred);
		int byteLeft = pageSize - virtualpageoffset;
		int amount = Math.min(byteLeft, length-numbyteTransferred);
		
		int physicalpageaddr = pageTable[virtualpagenumber].ppn* pageSize + virtualpageoffset; 
		System.arraycopy(memory, physicalpageaddr, data, offset+numbyteTransferred, amount);
		numbyteTransferred += amount;
	}

	return numbyteTransferred;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
	if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	
	// 
	int numbyteTransferred = 0;
	while(numbyteTransferred < length){
		int virtualpagenumber = Processor.pageFromAddress(vaddr + numbyteTransferred);
		if(virtualpagenumber < 0 || virtualpagenumber >= pageTable.length || pageTable[virtualpagenumber].readOnly){
			return 0;
		}
//		if(pageTable[virtualpagenumber] != null){
//			return -1;
//		}
		int virtualpageoffset = Processor.offsetFromAddress(vaddr + numbyteTransferred);
		int byteLeft = pageSize - virtualpageoffset;
		int amount = Math.min(byteLeft, length-numbyteTransferred);
		
		numbyteTransferred += amount;
	}
	
	numbyteTransferred = 0;
	while(numbyteTransferred < length){
		int virtualpagenumber = Processor.pageFromAddress(vaddr + numbyteTransferred);
		int virtualpageoffset = Processor.offsetFromAddress(vaddr + numbyteTransferred);
		int byteLeft = pageSize - virtualpageoffset;
		int amount = Math.min(byteLeft, length-numbyteTransferred);
		
		int physicalpageaddr = pageTable[virtualpagenumber].ppn* pageSize + virtualpageoffset; 
		System.arraycopy(data, offset+numbyteTransferred, memory, physicalpageaddr, amount);
		numbyteTransferred += amount;
	}

	return numbyteTransferred;
	//int amount = Math.min(length, memory.length-vaddr);
	//System.arraycopy(data, offset, memory, vaddr, amount);

	//return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}
	
	/*
	 * for loop to allocate pages. create a new translation entry for each allocated
	 */
	int page = -1;
	int ppn;
	
	for(int i = 0; i < this.numPages; i++) {
		page = UserKernel.allocatePage();
		if(page < 0) {
			//debug error
			
			/*
			 * deallocate all pages that were previously allocated 
			 */
			for(int j = 0; j < i; j++) {
				if(this.pageTable[j].valid) {
					UserKernel.deallocatePage(pageTable[j].ppn);
					pageTable[j].valid = false;
				}
				
				//on error close coff??
				coff.close();
				return false;
			}
		}
		
		//vpn, ppn, valid, readyonly, used, dirty
		pageTable[i] = new TranslationEntry(i, page, true, false, false, false);
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;
		ppn = this.pageTable[vpn].ppn;

		// for now, just assume virtual addresses=physical addresses
		section.loadPage(i, vpn);
		if(section.isReadOnly()) pageTable[vpn].readOnly = true;
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	for(int i = 0; i < pageTable.length; i++) {
    		if(pageTable[i].valid) {
    			//deallocate any pages. deallocatePages(pageTable[i].ppn);
    			//pageTable[i].valid = false;
    			UserKernel.deallocatePage(pageTable[i].ppn);
    			pageTable[i].valid = false;
    		}
    	}
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

//    if(id != 1){
//    	return -1;
//    }
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
    
    private int handleOpen(int name) {
    	String filename;
    	filename = readVirtualMemoryString(name,maxLength);
    	
    	if (filename == null) {
    		return -1;
    	}
    	
    	OpenFile f = ThreadedKernel.fileSystem.open(filename, false);
    	
    	if (f == null) {
    		return -1;
    	}
    	if(deletedfiles.contains(filename)) {
    		return -1;
    	}
    	return manger.add(f); 
    	
    }
    
    private int handleCreat(int name){
    	String filename;
    	filename = readVirtualMemoryString(name,maxLength);
    	if (filename == null) {
    		return -1;
    	}
    	if (deletedfiles.contains(filename)) {
    		return -1;
    	}
    	OpenFile f = ThreadedKernel.fileSystem.open(filename, true);
    	if (f == null) {
    		return -1;
    	}
    	
    	return manger.add(f); 
    	
    	
    }
    
    private int HandleRead(int fileDescriptor, int buffer, int count) {
    	
    	OpenFile f = manger.get(fileDescriptor); 
    	
    	if (f == null) {
    		return -1;
    	}
    	
    	if (buffer < 0 || buffer > Machine.processor().getMemory().length) {
    		return -1;
    	} 
    	
    	byte[] bytes = new byte[count];
    	int num_bytes = f.read(bytes,0, count); 
    	if (num_bytes == -1) {
    		return -1;
    	}
    	int length = writeVirtualMemory(buffer, bytes, 0, num_bytes);
    	return length;	
    	
    }
    
    private int HandleWrite(int fileDescriptor, int buffer, int count) {
    	
    	OpenFile f = manger.get(fileDescriptor); 
    	
    	if (f == null) {
    		return -1;
    	}
    	
    	if (buffer < 0 || buffer > Machine.processor().getMemory().length) {
    		return -1;
    	} 
    	
    	byte[] bytes = new byte[count];
    	int length = readVirtualMemory(buffer, bytes, 0, count);
    	if(length != count) {
    		return -1;
    	}
    	int num_bytes = f.write(bytes,0, count); 	
    	return num_bytes;	
    	
    }
    
    private int HandleClose(int fd) {
    	return manger.close(fd);
    }
    
    private int HandleUnlink(int name){
    	String filename;
    	filename = readVirtualMemoryString(name,maxLength);
    	
    	if (filename == null) {
    		return -1;
    	}
    	if(files.containsKey(filename)) {
    		deletedfiles.add(filename);
    	}
    	else {
        	boolean removed = false;   	
        	removed = ThreadedKernel.fileSystem.remove(filename);
        	if(removed == false) {
        		return -1;
        	}
    	}    	
    	return 0;
    }


    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallOpen:
		return handleOpen(a0);
	case syscallCreate:
		return handleCreat(a0);
	case syscallRead:
		return HandleRead(a0, a1, a2);
	case syscallWrite:
		return HandleWrite(a0, a1, a2);
	case syscallClose:
		return HandleClose(a0);
	case syscallUnlink:
		return HandleUnlink(a0);
	case syscallExec:
		return HandleExec(a0, a1, a2);
	case syscallJoin:
		return HandleJoin(a0, a1);
	case syscallExit:
		return HandleExit(a0);
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    private int HandleExit(int a0) {    	
		exitStatus = a0;
		/*
		 * Close all files here
		 */
		Lib.debug(dbgProcess, "hue");
		if(!manger.closeAll()) {
			Lib.debug(dbgProcess, "Closing files");
			//error?
			return -1;
		}
		
		unloadSections();
		
		/*
		 * destroy any processes. and terminate kernel if empty, then finish thread
		 */
//		for(int i = 0; i < childProcesses.size(); i++) {
//			childProcesses.get(i).process.handleExit;
//		}

		
		
		
		if(this.activeProcesses == 1) {
			Lib.debug(dbgProcess, "ONLY THING, TERMINATING");
			coff.close();
			Kernel.kernel.terminate();
			Lib.debug(dbgProcess, "Called");
			
		} else {
			Lib.debug(dbgProcess, "Finished call");
			UThread.finish();
			this.activeProcesses--;
			this.joinSemaphore.V();
		}
		return a0;
	}

	private int HandleJoin(int pid, int status) {
		 //TODO Auto-generated method stub
		ChildProcess child;
		if(pid < 0) {
			//debug
			return -1;
		}
		
		if(status < 0) {
			//debug
			return -1;
		}
		
		for(int i = 0; i < childProcesses.size(); i++) {
			if(childProcesses.get(i).id == pid) {
				child = childProcesses.get(i);
				child.process.joinSemaphore.P();
			} else {
				
				//DNE does not exist
				return -1;
			}
		}
		
		/*
		 * should join the child here then remove the child from the linked list.
		 */
		
		
		// TODO Auto-generated method stub
		return 0;
	}

	private int HandleExec(int file, int argc, int argv) {
			boolean error;
			String arguments[];
			String fileName;
			byte[] buf = new byte[4];
			UserProcess child;
			
			if(argc < 0) {
				Lib.debug(dbgProcess, "In exec, argc is < 0");
				//debug
				return -1;
			}
			
			if(argv < 0) {
				Lib.debug(dbgProcess, "In exec, argv is < 0");
				//debuyg
				return -1;
			}
			
			fileName = this.readVirtualMemoryString(file,  256);
			
			//check filename extenstion .coff?
			if(fileName == null) {
				Lib.debug(dbgProcess, "In exec, filename is null");
				return -1;
			}
			
			arguments = new String[argc];
			
			for(int i = 0; i < argc; i++) {
				if(this.readVirtualMemory(argv + (i * 4), buf) != 4) {
					//invalid transfer
					Lib.debug(dbgProcess, "In exec, RVM is != 4");
					return -1;
				}
				
				arguments[i] = this.readVirtualMemoryString(Lib.bytesToInt(buf, 0), 256);
				if(arguments[i] == null) {
					//debug
					Lib.debug(dbgProcess, "In exec, argument" + i + " null");
					return -1;
				}
			}
			child = UserProcess.newUserProcess();
			ChildProcess cp = new ChildProcess(child, child.id,this);
			childProcesses.add(cp);
			
			if(!child.execute(fileName, arguments)) {
				Lib.debug(dbgProcess, "In exec, child failed to execute.");
				return -1;
			}
			
			return child.id;
	}

	/**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    public class DescriptorManager{
    	int size = 0;
    	OpenFile fileOpen[] = new OpenFile[maxFile]; 
    	
    	public boolean closeAll() {
    		for(int i = 0; i < size; i++) {
    			if(this.close(i) < 0) {
    				return false;
    			}
    		}
    		
    		return true;
    	}
    	public int add(int index, OpenFile file) {
  
    		if(fileOpen[index] == null) {
				fileOpen[index] = file;
				if (files.get(file.getName()) == null) {
					files.put(file.getName(), index);
					size++;
					return index;
				}
			}
    		return -1;
    	}
    	
    	public int add(OpenFile file) {
//    		for (int i = 0; i < maxFile;i++) {
//        		if (file != null){
//        			fileOpen[i] = file;
//        			if(files.get(file.getName()) == null) {
//        				files.put(file.getName(), i);
//        				return i;
//        			}		
//        		}
//    		}
//    		return -1;
    		
    		for(int index = 0; index < maxFile; index++) {
    			if(fileOpen[index] == null) {
    				return add(index,file);
    			}
    			
    		}
    		return -1;

    	}
    	
    	public int close(int fd){
    		if(fileOpen[fd] == null) {
    			return -1;
    		}
    		if (fd < 0 || fd >= maxFile) {
    			return -1;
    		} 
    		OpenFile f = fileOpen[fd];
    		fileOpen[fd] = null;
    		String fName = f.getName();
    		f.close();
    		files.remove(fName);
    		if(deletedfiles.contains(fName)) {
    			deletedfiles.remove(fName);
    			ThreadedKernel.fileSystem.remove(fName);
    		}
    			    		
    		return 0;    		
    	}
    	
        public  OpenFile get(int fd){
    		if(fd < 0 || fd >= maxFile)
    			return null;
    		return fileOpen[fd];
    	}
    }
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    protected static Hashtable<String,Integer> files = new Hashtable<String, Integer>(); 
    protected static HashSet<String> deletedfiles = new HashSet<String>();
    protected static int processID = 0;
    private int id;
    protected static int maxFile = 16;
    protected static int maxLength = 256;
    protected DescriptorManager manger;
    private int initialPC, initialSP;
    private int argc, argv;
    public static boolean rootExists = false;
    private int exitStatus;
    public Semaphore joinSemaphore = new Semaphore(0);
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    protected static LinkedList<Integer> activeProcess = new LinkedList<Integer>();
    
    protected static UThread UserThread;
    public UserProcess parent= null;
    static int activeProcesses = 0;
}