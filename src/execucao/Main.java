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
		Registro r5 = new Registro(22, "Vitu");
		int chaveTeste = 0;
		String contTeste;
		int proxTeste = 10;
		int antTeste = 10;

		GerenciadorDeArquivo gerenciador = new GerenciadorDeArquivo(f);
//		gerenciador.insereRegistro(r2);
//		gerenciador.insereRegistro(r3);
//		gerenciador.insereRegistro(r1);
//		gerenciador.insereRegistro(r4);
		gerenciador.insereRegistro(r5);
		gerenciador.imprimeArquivo();
//		System.out.println(gerenciador.debug(0));
	}
}