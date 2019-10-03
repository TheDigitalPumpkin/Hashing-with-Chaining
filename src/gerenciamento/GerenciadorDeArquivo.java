package gerenciamento;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;

import execucao.Main;
import registro.Registro;

public class GerenciadorDeArquivo {
	protected RandomAccessFile arq;
	private final int TAMANHO_ARQUIVO = Main.TAMANHO_ARQUIVO;
	private int cabecaListaVazia;
	private long[] tabelaDeIndices = new long[TAMANHO_ARQUIVO];
	
	public GerenciadorDeArquivo(File arq) {		
		try {					
			this.arq = new RandomAccessFile(arq, "rw");	
			int inicializadorListaVazia;	
			
			if(this.arq.length() > 0) {
				for(int i = 0; i < TAMANHO_ARQUIVO * 32; i += 32) {
					this.arq.seek(i);
					inicializadorListaVazia = this.arq.readInt();
					
					if(inicializadorListaVazia == -1) {
						cabecaListaVazia = i / 32;
						break;
					} 
				}
				
				for(int i = 0; i < TAMANHO_ARQUIVO * 32; i += 32) {
					this.arq.seek(i);
					int chaveLida = this.arq.readInt();
					
					if(chaveLida != -1) {
						tabelaDeIndices[chaveLida % TAMANHO_ARQUIVO]++;
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
//					System.out.println("Proximo = " + proximo);
					
					if(proximo == -1) {
						break;
					}
					
					arq.seek(proximo * 32);
					
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
				tabelaDeIndices[hashRegistro]++;
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
//						System.out.println("Proximo na cadeia: " + proximo);
						
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
					int novaCabeca = arq.readInt();
//					System.out.println("Nova cabeca: " + novaCabeca);
					arq.seek(arq.getFilePointer() - 4);
					arq.writeInt(-1);
//					System.out.println("Anterior = " + anterior);
					arq.seek((anterior * 32) + 28);
//					System.out.println("Vai escrever prox = " + cabecaListaVazia + " no ponteiro " + arq.getFilePointer());
					arq.writeInt(cabecaListaVazia);		
					cabecaListaVazia = novaCabeca;
					tabelaDeIndices[hashRegistro]++;
				}
				
				//1o: Escrever na posicao cabecaListaVazia.
				//2o: Inserir chave na devida posicao.
				//3o: Atualizar ponteiros da chave que foi movida.
				
				//Se a chave nao tiver o mesmo hash, devemos mover a chave armazenada aqui para outra posicao
				else {
				//Movemos o conteudo armazenado na posicao para uma nova posicao:
					//1o: Gravamos o conteudo:
					arq.seek(hashRegistro * 32); 
					int chaveParaMover = arq.readInt();
					byte[] buffer = new byte[20];
					arq.read(buffer);
					String conteudoParaMover = new String(buffer);
					arq.seek((hashRegistro * 32) + 24);
					int ponteiroAnteriorParaMover = arq.readInt();
					int ponteiroProxParaMover = arq.readInt();
					
					//2o: Movemos para cabecaListaVazia:
					arq.seek(cabecaListaVazia * 32);
					arq.writeInt(chaveParaMover);
					arq.writeBytes(conteudoParaMover);
					arq.seek((cabecaListaVazia * 32) + 24);
					arq.writeInt(ponteiroAnteriorParaMover);
					
					//A variavel antigaCabecaListaVazia serve para retornarmos a posicao do registro movido para atualizarmos seus ponteiros.
					int antigaCabecaListaVazia = cabecaListaVazia; 
					
					//Esta variavel armazena a posicao para onde o registro sera movido.
					int novaCabecaListaVazia = arq.readInt(); 
					
					arq.seek(arq.getFilePointer() - 4);					
					arq.writeInt(ponteiroProxParaMover);
					cabecaListaVazia = novaCabecaListaVazia;
					
				//Inserimos o novo registro em sua devida posi��o:
					arq.seek(hashRegistro * 32);
					arq.writeInt(reg.getChave());
					arq.writeBytes(reg.getConteudo());
					arq.seek((hashRegistro * 32) + 24);
					arq.writeInt(-1);
					arq.writeInt(-1);
					tabelaDeIndices[hashRegistro]++;
					
				//Atualizamos os ponteiros do registro que movemos para abrir espaco para o registro de hash correto
				//Para isso, consultamos os ponteiros anterior e proximo do registro que movemos.
					arq.seek((antigaCabecaListaVazia * 32) + 24);
					int registroAnteriorParaAtualizar = arq.readInt();
					int proximoRegistroParaAtualizar = arq.readInt();
					
					if(registroAnteriorParaAtualizar > 0) {
						arq.seek((registroAnteriorParaAtualizar * 32) + 28);
						arq.writeInt(antigaCabecaListaVazia);
					}
					
					if(proximoRegistroParaAtualizar > 0) {
						arq.seek((proximoRegistroParaAtualizar * 32) + 24);
						arq.writeInt(antigaCabecaListaVazia);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void consultaChave(int chave) {
		int hash = chave % TAMANHO_ARQUIVO;
		byte[] buffer = new byte[20];
		
		try {
			arq.seek(hash * 32);
			
			if(arq.readInt() == -1) {
				System.out.println("chave nao encontrada: " + chave);
				return;
			}
			
			arq.seek(hash * 32);
			
			if(arq.readInt() == chave) {
				System.out.println("chave: " + chave);
				arq.read(buffer);
				String conteudo = new String(buffer);
				conteudo = conteudo.toLowerCase();
				System.out.println(conteudo);
				return;
			}
			
			else {
				arq.seek((hash * 32) + 28);
				int proximo = arq.readInt();
				
				if(proximo == -1) {
					System.out.println("chave nao encontrada: " + chave);
					return;
				}
				
				else { 
					while(true) {
						arq.seek(proximo * 32);
						
						if(arq.readInt() == chave) {
							System.out.println("chave: " + chave);
							arq.read(buffer);
							String conteudo = new String(buffer);
							conteudo = conteudo.toLowerCase();
							System.out.println(conteudo);
							return;
						}
						
						else {
							arq.seek((proximo * 32) + 28);
							proximo = arq.readInt();
							
							if(proximo == -1) {
								System.out.println("chave nao encontrada: " + chave);
								return;
							}
						}
					}
				}
			}
		} catch(IOException ex) {
			System.out.println("Erro ao consultar chave.");
			ex.printStackTrace();
		}
	}

	public void removeChave(int chaveParaRemover) {
		int hash = chaveParaRemover % TAMANHO_ARQUIVO;
		int proximo = -2;
		
		try {
			arq.seek(hash * 32);
			
			while(proximo != -1) {
				int chave = arq.readInt();
				
			//Para removermos uma determinada chave, supondo que a mesma esteja no arquivo
			//devemos:
				//1o: Encontrar a chave
				//2o: Ao encontrarmos, verificamos seus ponteiros.
				//3o: Apagamos o registro.
				//4o: Atualizamos os ponteiros.
				
				//Encontramos a chave
				if(chave == chaveParaRemover) {
					//Verificamos seus ponteiros.
					arq.seek(arq.getFilePointer() + 20);
					int ponteiroAnterior = arq.readInt();
					int proximoPonteiro = arq.readInt();
//					System.out.println("Anterior = " + ponteiroAnterior + ", Proximo = " + proximoPonteiro);
					arq.seek(arq.getFilePointer() - 32);
					long posicaoAtual = arq.getFilePointer() / 32;
					arq.writeInt(-1);
					arq.writeBytes("                    ");
									
					arq.seek((cabecaListaVazia * 32) + 24);
					
					//Inserimos a nova posicao vazia na lista de posicoes vazias.
					int antListaVazia = arq.readInt();
					int proxListaVazia = arq.readInt();
					arq.seek((posicaoAtual * 32) + 24);
					arq.writeInt(antListaVazia);
					arq.writeInt(proxListaVazia);
					
					if(posicaoAtual < cabecaListaVazia) {
						arq.seek((cabecaListaVazia * 32) + 24);
						arq.writeInt((int) posicaoAtual);
						arq.seek((posicaoAtual * 32) + 28);
						arq.writeInt(cabecaListaVazia);
						cabecaListaVazia = (int) posicaoAtual;
					}

					if(ponteiroAnterior != -1 && proximoPonteiro != -1) {
						arq.seek((ponteiroAnterior * 32) + 28);
						arq.writeInt(proximoPonteiro);
						arq.seek((proximoPonteiro * 32) + 24);
						arq.writeInt(ponteiroAnterior);
						return;
					} else if(ponteiroAnterior != -1) {
						arq.seek((ponteiroAnterior * 32) + 28);
						arq.writeInt(-1);
						arq.seek(posicaoAtual * 32); 
						return;
					} else if(proximoPonteiro != -1) {
						//Como estamos removendo o primeiro elemento da lista, devemos arrastar o segundo elemento para a sua posicao
						//No proximo bloco de codigo, estamos copiando o conteudo da segunda posicao para transferirmos mais tarde.
						System.out.println("Removendo primeiro da lista.");
						arq.seek(proximoPonteiro * 32);
						int chaveParaMover = arq.readInt();
						byte[] buffer = new byte[20];
						arq.read(buffer);
						String conteudoParaMover = new String(buffer);
						arq.seek((proximoPonteiro * 32) + 28);
						int proxPonteiroParaMover = arq.readInt();
						
						//Agora apagamos o proximo da cadeia da sua posicao e o movemos para a posicao do hash original
						arq.seek(proximoPonteiro * 32);
						arq.writeInt(-1);
						arq.writeBytes("                    ");
						arq.writeInt(antListaVazia);
						arq.writeInt(proxListaVazia);
						
						arq.seek((proxPonteiroParaMover * 32) + 24);
						arq.writeInt(hash);		
						arq.seek((hash * 32));
						arq.writeInt(chaveParaMover);
						arq.writeBytes(conteudoParaMover);
						arq.seek((hash * 32) + 24);
						arq.writeInt(-1);
						arq.writeInt(proxPonteiroParaMover);
						return;
					} else if(ponteiroAnterior == -1 && proximoPonteiro == -1) {
						//Fazer remocao para quando lista so tem um elemento.
					}
				}
				
				arq.seek(arq.getFilePointer() + 24);
				proximo = arq.readInt();
				arq.seek(proximo * 32);
			}
			
			System.out.println("chave nao encontrada: " + chaveParaRemover);
		} catch(IOException ex) {
			System.out.println("Erro ao remover registro.");
			ex.printStackTrace();
		}
	}
	
	public double calculaMedia() {
		long somatorio = 0;
		double mediaDeAcessos;
		
		for(int i = 0; i < tabelaDeIndices.length; i++) {
			if(tabelaDeIndices[i] > 0) {
				somatorio += somatorio(tabelaDeIndices[i]);
			}
		}
		
		mediaDeAcessos = somatorio / TAMANHO_ARQUIVO;
		
		return BigDecimal.valueOf(mediaDeAcessos).setScale(1).doubleValue();
	}

	private long somatorio(long num) {
		long soma = 0;
		
		if(num == 1) {
			soma = 1;
		} else {
			for(long i = num; i >= 0; i--) {
				soma += i;
			}
		}
		
		return soma;
	}
	
	public void imprimeTabela() {
		System.out.println("Tabela de indices: ");
		
		for(int i = 0; i < tabelaDeIndices.length; i++) {
			System.out.print(tabelaDeIndices[i] + " ");
		}
	}
	
	public void imprimeArquivo() {
		int apontador1, apontador2 = -2;
		
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
			
					byte[] buffer = new byte[20];
					arq.read(buffer);
					String s = new String(buffer);
					s = s.toLowerCase();
					s = s.replaceAll("\\s+", "");
					System.out.print(s);
					
					arq.seek(i + 24);
					apontador1 = arq.readInt();
					apontador2 = arq.readInt();
					System.out.print(" apontador1: ");
					
					if(apontador1 == -1) {
						System.out.print("nulo");
					} 
					
					else System.out.print(apontador1);
					
					System.out.print(" apontador2: ");
					
					if(apontador2 == -1) {
						System.out.print("nulo\n");
					}
					
					else System.out.print(apontador2 + "\n");
				}
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		}
		
		System.out.println("posicao inicial da lista de posicoes vazias: " + cabecaListaVazia);
	}
}
