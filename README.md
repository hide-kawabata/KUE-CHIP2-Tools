# KUE-CHIP2 Tools

KUE-CHIP2 is a tiny 8-bit processor [1][2].
Here we present a set of tools for helping beginners, e.g., high school students, get a feel of the KUE-CHIP2 CPU. We are planning to prepare an instruction set-level symulator (interpreter) and an assembler. All tools are written in Kotlin, so that you can execute those tools on any environment with the Java Rutime Environment.

[1] H. Kanbara and H. Yasuura, KUE-CHIP2: A microprocessor for education of LSI design and computer hardware, Proc. Synthesis and System Integration of Mixed Technologies (SASIMI'95) pp.233-240, 1995.
[2] H. Kanbara, KUE-CHIP: A Microprocessor for education of Computer Architecture and LSI design, Proc. IEEE ASIC Seminar and Exhibit, 1990.

## KUE-CHIP2 Assembler

   ```
$ cat mul.asm
DATA1:  EQU     80H
DATA2:  EQU     81H
ANS:    EQU     90H
        LD      IX,     [DATA2]
        EOR     ACC,    ACC
LABEL:  ADD     ACC,    [DATA1]
        SUB     IX,     1
        BNZ     LABEL
        ST      ACC,    [ANS]
        HLT
        END
$ ./kas mul.asm
 80 :                   DATA1:  EQU     80H
 81 :                   DATA2:  EQU     81H
 90 :                   ANS:    EQU     90H
 00 :   6C 81                   LD      IX,     [DATA2]
 02 :   C0                      EOR     ACC,    ACC
 03 :   B4 80           LABEL:  ADD     ACC,    [DATA1]
 05 :   AA 01                   SUB     IX,     1
 07 :   31 03                   BNZ     LABEL
 09 :   74 90                   ST      ACC,    [ANS]
 0B :   0F                      HLT
                                END
$
   ```


## KUE-CHIP2 Symulator

Under construction.