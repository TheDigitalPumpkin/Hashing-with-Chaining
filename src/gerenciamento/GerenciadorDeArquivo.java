package gerenciamento;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import execucao.Main;
import registro.Registro;

public class GerenciadorDeArquivo {
	protected static RandomAccessFile arq;
	private static int TAMANHO_ARQUIVO = Main.TAMANHO_ARQUIVO;
	
	public GerenciadorDeArquivo(File arq) {		
		try {					
			this.arq = new RandomAccessFile(arq, "rw");		
			
			if(this.arq.length() == 0) {
				this.arq.setLength(TAMANHO_ARQUIVO * 32);
				
				for(int i = 0; i < TAMANHO_ARQUIVO * 32; i += 32) {
					this.arq.seek(i);
					this.arq.writeInt(-1);
				}
			}
		} 
		
		catch(IOException ex) {
			System.out.println("Erro ao criar o gerenciador de arquivos");
			ex.printStackTrace();
		}		
	}
	
	public void insereRegistro(Registro reg) {
		int hashRegistro = reg.hash(TAMANHO_ARQUIVO);
		long anterior = -1;
		int chave;
		
		try {
			arq.seek(hashRegistro * 32);
//			System.out.println("Hash leva a pos " + hashRegistro + " do arquivo, cujo ponteiro e: " + arq.getFilePointer());
			chave = arq.readInt();
			
			if(chave == reg.getChave()) {
				System.out.println("chave ja existente: " + reg.getChave());
				return;
			}
			
			if(chave == -1) {
				arq.seek(hashRegistro * 32);
				arq.writeInt(reg.getChave());
				arq.writeBytes(reg.getConteudo());
				arq.writeInt(reg.getAnt());
				arq.writeInt(reg.getProx());
			}
			
			else {				
				for(long i = arq.length() - 32; i >= 0; i -= 32) {
					anterior = arq.getFilePointer();
					arq.seek(i);
					chave = arq.readInt();
					
					if(chave == -1) {
						arq.seek(i);
						arq.writeInt(reg.getChave());
						arq.writeBytes(reg.getConteudo());
						arq.writeInt((int) anterior);
						arq.writeInt(reg.getProx());
						long pos = i / 32;
						int posicaoInserida = (int) pos;
						
						arq.seek(anterior);
						arq.readInt();
						
						//Le enquanto for string.
						int track = arq.read();
						
						while(track >= 33 && track <= 126) {
							track = arq.read();
						}
						
						arq.readInt();
						arq.writeInt(posicaoInserida);
						
						return;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean consultaChave(int chave) {
		int hash = chave % TAMANHO_ARQUIVO;
		boolean encontrado = false;
		
		try {
			arq.seek(hash * 32);
			
			if(arq.readInt() == chave) 
				encontrado = true;
				
		} catch(IOException ex) {
			ex.printStackTrace();
		} 
		
		return encontrado;
	}
	
	public int debug(int chave) {
		int ret = -1;
		
		try {
			arq.seek((chave % TAMANHO_ARQUIVO) * 32);
			ret = arq.readInt();
			System.out.println(arq.read());
		} catch(IOException ex) {
			ex.printStackTrace();
		}
		
		return ret;
	}
	
	private void escreveRegistroEmArquivo(Registro reg) {
		try {
			arq.writeInt(reg.getChave());
			arq.writeBytes(reg.getConteudo());
			arq.writeInt(reg.getAnt());
			arq.writeInt(reg.getProx());
		} catch (IOException e) {
			System.out.println("Erro: funcao escreveRegistroEmArquivo");
			e.printStackTrace();
		}
	}
}
