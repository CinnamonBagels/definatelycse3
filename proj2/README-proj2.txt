Steven Yeu A10720198
Shiyao Liu A10626758
Samuel Ko A09587224

PROJCT 2 Synopsis

////////
Task 1
////////

In order to implement this properly, we needed to create a File Descriptor to manage all open files, so that they could be referenced at a later time.

For creat():
Read file name, and use fileSystem.open to create/open file and return.

open():
Read file name, and use fileSystem.open to open file and return


read():
get file from descriptor, write the contents to the buffer, return length read.

write():
get file from descriptor, read from buffer, then write to stream


close():
get file from descriptor, check if fil valid, more checking, then finally close and add to a deleted list so it cannot be modified

error checking: 
file == null || file # is invalid.

unlink():
we check if the file actually marked for deltion, then we delete it.


	*TESTING*
	We used the TA tests, and we also created a few tests for unlink(), by modifying the TA tests to fit our own needs.
////////
Task 2
////////

readVirtualMemory():
read up to the offset and repeat until we reach the maximum amount.

writeVirualMemory():
write up to th offset and repeat until we reach to the maximum amount.

loadSection():
allocate a new page to pageTable using a linked list

unloadSection():
deallocate all the page in pageTable to a freePage linkedlist

	*TESTING*
	TA Tests
////////
Task 3
////////

exec():
get file name, load then if cant execute return error, else return child pid, add child to list of childproceses
join():
calld joinSemaphore.P when join is called, joinSemaphore.V when handleExit calld in child.
exit():
store exit status locally in process, if last process, just kernel teerminate, else Uthread.finish(). Cut links betwen parent/children.

Testing

TA Test
