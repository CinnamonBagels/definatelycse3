#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main(int argc, char** argv)
{
   int fd2;
   int fd;
   int fd3;
   char* fn;
   fn = "myfile.txt";
   fd = open(fn); // similarly replace with open()
   fd3 = close(fd);   
   fd2 = unlink(fn);
   halt(); //can change this line to return 0; after implementing exit() sys
}
