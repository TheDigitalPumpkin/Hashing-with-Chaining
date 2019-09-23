package execucao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import gerenciamento.GerenciadorDeArquivo;
import registro.Registro;

public class Main {
	
	public static int TAMANHO_ARQUIVO = 11;

	public static void main(String[] args) {		
		
		File f = new File("arquivo.dat");	
		Registro r1 = new Registro(15, "Bruno");
		Registro r2 = new Registro(33, "Roque");
		Registro r3 = new Registro(26, "Levy");
		Registro r4 = new Registro(37, "Jason");
		Registro r5 = new Registro(48, "Vitu");
		Registro r6 = new Registro(16, "Kappa");
		Registro r7 = new Registro(14, "Poggers");
		Registro r8 = new Registro(28, "Pepe");
		
		GerenciadorDeArquivo gerenciador = new GerenciadorDeArquivo(f);
//		gerenciador.insereRegistro(r2);
//		gerenciador.consultaChave(15);
//		gerenciador.insereRegistro(r3);
//		gerenciador.insereRegistro(r1);
//		gerenciador.insereRegistro(r4);
//		gerenciador.insereRegistro(r5);
//		gerenciador.insereRegistro(r6);
//		gerenciador.insereRegistro(r7);
//		gerenciador.insereRegistro(r8);
//		gerenciador.consultaChave(15);
		System.out.println(gerenciador.getCabeca());
		gerenciador.imprimeArquivo();
//		System.out.println(gerenciador.debug(0));
	}
}