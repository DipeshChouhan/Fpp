SECTION .data
age: dq 0
age1: dq 0
SECTION .text
global _start
print:
      mov     r9, -3689348814741910323
      sub     rsp, 40
      mov     BYTE [rsp+31], 10
      lea     rcx, [rsp+30]
  .L2:
      mov     rax, rdi
      lea     r8, [rsp+32]
      mul     r9
      mov     rax, rdi
      sub     r8, rcx
      shr     rdx, 3
      lea     rsi, [rdx+rdx*4]
      add     rsi, rsi
      sub     rax, rsi
      add     eax, 48
      mov     BYTE [rcx], al
      mov     rax, rdi
      mov     rdi, rdx
      mov     rdx, rcx
      sub     rcx, 1
      cmp     rax, 9
      ja      .L2
      lea     rax, [rsp+32]
      mov     edi, 1
      sub     rdx, rax
      xor     eax, eax
      lea     rsi, [rsp+32+rdx]
      mov     rdx, r8
      mov     rax, 1
      syscall
      add     rsp, 40
      ret


_start:
mov rdx, 2
mov rbx, 3
add rdx, rbx
mov [age], DWORD (2 + 3)
mov rdi, [age]
call print
mov rdx, 10
mov [age1], rdx
mov rdi, [age1]
call print
mov rbx, 0  ; return 0 status on exit _ 'No Errors'
mov rax, 1  ; invoke SYS_EXIT (kernel opcode 1)
int 80h

