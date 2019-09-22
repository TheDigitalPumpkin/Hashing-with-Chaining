package gerenciamento;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import execucao.Main;
import registro.Registro;

public class GerenciadorDeArquivo {
	protected RandomAccessFile arq;
	private final int TAMANHO_ARQUIVO = Main.TAMANHO_ARQUIVO;
	private int cabecaListaVazia;
	
	public GerenciadorDeArquivo(File arq) {		
		try {					
			this.arq = new RandomAccessFile(arq, "rw");	
			//Essa linha trunca o arquivo quando necessario, para propositos de debug.
			//this.arq.setLength(0);
			int inicializadorListaVazia;
			
			if(this.arq.length() > 0) {
				for(int i = 0; i < TAMANHO_ARQUIVO * 32; i += 32) {
					this.arq.seek(i);
					inicializadorListaVazia = this.arq.readInt();
					
					if(inicializadorListaVazia == -1) {
						cabecaListaVazia = i / 32;
						System.out.println("Cabeca comecou como " + cabecaListaVazia);
						break;
					}
				}
			}
			
			if(this.arq.length() < TAMANHO_ARQUIVO * 32) {
				this.arq.setLength(TAMANHO_ARQUIVO * 32);
				int anteriorInit = 10, proximoInit = 1;
				cabecaListaVazia = 0;
				
				for(int i = 0; i < TAMANHO_ARQUIVO * 32; i += 32) {
					if(i == (TAMANHO_ARQUIVO * 32) - 32) {
						proximoInit = 0;
					}
					
					this.arq.seek(i);
					this.arq.writeInt(-1);
					this.arq.seek(this.arq.getFilePointer() + 20);
					this.arq.writeInt(anteriorInit);				
					this.arq.writeInt(proximoInit);
					anteriorInit++;	proximoInit++;
					
					if(i == 0) {
						anteriorInit = 0;
					}
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
		long anterior, proximo = -2;
		int chave;
		
		try {
			arq.seek(hashRegistro * 32);
			chave = arq.readInt();
			arq.seek(hashRegistro * 32);
			
			if(chave == reg.getChave()) {
				System.out.println("chave ja existente: " + reg.getChave());
				return;
			}
			
			else if(chave != -1 && (chave % TAMANHO_ARQUIVO) == hashRegistro) {
				while(true) {
					arq.seek(arq.getFilePointer() + 28);
					proximo = arq.readInt();
					System.out.println("Proximo = " + proximo);
					
					if(proximo == -1) {
						break;
					}
					
					arq.seek(proximo * 32);
					
//					if(arq.readInt() != hashRegistro) {
//						break;
//					}
					
//					arq.seek(arq.getFilePointer() - 4);
					
					if(arq.readInt() == reg.getChave()) {
						System.out.println("chave ja existente: " + reg.getChave());
						return;
					}
					
					arq.seek(arq.getFilePointer() - 4);
				}
			}
			//Caso primeiro hash for vazio.
			if(chave == -1) {
				if(hashRegistro == cabecaListaVazia) {
//					System.out.println("Entrou aqui");
					arq.seek((hashRegistro * 32) + 28);
					cabecaListaVazia = arq.readInt();
//					System.out.println("Cabeca: " + cabecaListaVazia);
				}
				//Primeiro, obtemos as posicoes vazias anteriores e proximas
				arq.seek((hashRegistro * 32) + 24);
				anterior = arq.readInt();
				proximo = arq.readInt();
				//Depois, escrevemos o registro no arquivo
				arq.seek(hashRegistro * 32);
				arq.writeInt(reg.getChave());
				arq.writeBytes(reg.getConteudo());
				arq.seek((hashRegistro * 32) + 24);
				arq.writeInt(reg.getAnt());
				arq.writeInt(reg.getProx());
				//Agora atualizamos as posicoes vazias
				arq.seek((anterior * 32) + 28);
				arq.writeInt((int) proximo);
				
				arq.seek((proximo * 32) + 24);
				arq.writeInt((int) anterior);
			}
			
			//Caso ja exista uma chave na posicao do hash
			else {		
				//Se a chave tiver mesmo hash
				if((chave % TAMANHO_ARQUIVO) == hashRegistro) {
//					System.out.println("Mesmo hash");
					arq.seek(hashRegistro * 32);
					
					while (true) {
						anterior = arq.getFilePointer() / 32;
						arq.seek(arq.getFilePointer() + 28);
						proximo = arq.readInt();
						System.out.println("Proximo na cadeia: " + proximo);
						
						if(proximo == -1) {
							break;
						}
						
						arq.seek(proximo * 32);
					}
						
//					anterior = arq.getFilePointer() - 32;
					System.out.println("Cabeca: " + cabecaListaVazia);
					arq.seek(cabecaListaVazia * 32);
					arq.writeInt(reg.getChave());
					arq.writeBytes(reg.getConteudo());
					arq.seek((cabecaListaVazia * 32) + 24);
					arq.writeInt((int) anterior);
					arq.writeInt(-1);
//					System.out.println("Anterior = " + anterior);
					arq.seek((anterior * 32) + 28);
//					System.out.println("Vai escrever prox = " + cabecaListaVazia + " no ponteiro " + arq.getFilePointer());
					arq.writeInt(cabecaListaVazia);								
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
	
	public void imprimeArquivo() {
		for(long i = 0; i < TAMANHO_ARQUIVO * 32; i += 32) {
			System.out.print((i / 32) + ": ");
			try {
				arq.seek(i);
				
				if(arq.readInt() == -1) {
					System.out.print("apontador1: ");
					arq.seek(i + 24);
					System.out.print(arq.readInt());
					System.out.print(" apontador2: " + arq.readInt() + "\n");
				}
				
				else {
					arq.seek(i);
					System.out.print(arq.readInt() + " ");
					

					byte[] string = new byte[20];
					arq.read(string);
					String s = new String(string);
					System.out.print(s);
					
					arq.seek(i + 24);
					System.out.print(" apontador1: " + arq.readInt() + " apontador2: " + arq.readInt() + "\n");
				}
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		}
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
