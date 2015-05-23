#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main(int argc, char** argv)
{
 char* fn; char* argv2[1];
 int c, cid;
 fn = "loopSimple.coff";
 printf("1");
 argv2[0] = fn;
 printf("2");
 c = 1;
 printf("3");
 cid = exec(fn, c,argv2);
 printf("4");
 while(c++ < 15) {
 	printf("c=%d\n", c);
 }
 return 0;
}
