package execucao;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Scanner;

import gerenciamento.GerenciadorDeArquivo;
import registro.Registro;

public class Main {
	
	public final static int TAMANHO_ARQUIVO = 11;
	
	private static void realizarOperacoes(GerenciadorDeArquivo gerenciador) {
		String input, conteudo;
		char op;
		int chave;
		Scanner scanOp = new Scanner(System.in);
		Scanner scanConteudo = new Scanner(System.in);
		Scanner scanChave = new Scanner(System.in);
		
		input = scanOp.nextLine();
		op = input.charAt(0);
		
		while(op != 'e') {
			switch(op) {			
				case 'i':
					chave = scanChave.nextInt();
					conteudo = scanConteudo.nextLine();
					gerenciador.insereRegistro(new Registro(chave, conteudo));
					break;
					
				case 'c':
					chave = scanChave.nextInt();
					gerenciador.consultaChave(chave);
					break;
					
				case 'r':
					chave = scanChave.nextInt();
					gerenciador.removeChave(chave);
					break;
					
				case 'p':
					gerenciador.imprimeArquivo();
					break;
					
				case 'm':
					System.out.println(gerenciador.calculaMedia());
					break;
			}
			
			input = scanOp.nextLine();
			op = input.charAt(0);
		}
		
		scanOp.close();
		scanConteudo.close();
		scanChave.close();
	}

	public static void main(String[] args) {		
		File f = new File("arquivo.dat");	
		GerenciadorDeArquivo gerenciador = new GerenciadorDeArquivo(f);
		realizarOperacoes(gerenciador);		
	}
}