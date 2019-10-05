package gerenciamento;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.text.DecimalFormat;

import execucao.Main;
import registro.Registro;

public class GerenciadorDeArquivo {
	protected RandomAccessFile arq;
	private final int TAMANHO_ARQUIVO = Main.TAMANHO_ARQUIVO;
	private int cabecaListaVazia;
	private long[] tabelaDeIndices = new long[TAMANHO_ARQUIVO];
	private long qtdDeChaves;
	
	public GerenciadorDeArquivo(File arq) {		
		try {					
			this.arq = new RandomAccessFile(arq, "rw");	
			int inicializadorListaVazia;	
			
			//Caso o arquivo já exista (indicado por arq.length() > 0), realizamos algumas rotinas de inicialização.
			if(this.arq.length() > 0) {
				//Primeiro, buscamos uma posição vazia para ser a cabeça da lista de posições vazias.
				for(int i = 0; i < TAMANHO_ARQUIVO * 32; i += 32) {
					this.arq.seek(i);
					inicializadorListaVazia = this.arq.readInt();
					
					if(inicializadorListaVazia == -1) {
						cabecaListaVazia = i / 32;
						break;
					} 
				}
				
				//Inicializamos a tabela de indices.
				for(int i = 0; i < TAMANHO_ARQUIVO * 32; i += 32) {
					this.arq.seek(i);
					int chaveLida = this.arq.readInt();
					
					if(chaveLida != -1) {
						tabelaDeIndices[chaveLida % TAMANHO_ARQUIVO]++;
						qtdDeChaves++;
					}
				}
			}
			
			//Caso o arquivo não exista ou tiver um tamanho menor do que o tamanho requisitado, aumentamos o tamanho conforme o necessário, e inicializamos a lista de posições vazias.
			if(this.arq.length() < TAMANHO_ARQUIVO * 32) {
				this.arq.setLength(TAMANHO_ARQUIVO * 32);
				int anteriorInit = TAMANHO_ARQUIVO - 1, proximoInit = 1;
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
				//Este if corrige a atualização no caso do ponteiro anterior apontar para a própria posição, algo que pode acontecer após uma remoção.
				if(anterior != hashRegistro) {
					arq.seek((anterior * 32) + 28);							
					arq.writeInt((int) proximo);
				}
				
				arq.seek(anterior * 32);
				
				//Este if corrige a atualização no caso onde o ponteiro anterior aponta para uma posição que não esteja vazia.
				if(arq.readInt() != -1) {
					arq.seek((anterior * 32) + 28);
					arq.writeInt(-1);
				}
				
				arq.seek(proximo * 32);
				
				if(arq.readInt() == -1) {
					arq.seek(arq.getFilePointer() + 20);
					arq.writeInt((int) anterior);
				}
			
				tabelaDeIndices[hashRegistro]++;
				qtdDeChaves++;
			}
			
			//Caso ja exista uma chave na posicao do hash
			else {		
				//Se a chave tiver mesmo hash
				if((chave % TAMANHO_ARQUIVO) == hashRegistro) {
					arq.seek(hashRegistro * 32);
					
					//Procuramos o próximo na cadeia até encontrarmos um registro cujo próximo ponteiro não aponta para ninguém.
					while (true) {
						anterior = arq.getFilePointer() / 32;
						arq.seek(arq.getFilePointer() + 28);
						proximo = arq.readInt();
						
						if(proximo == -1) {
							break;
						}
						
						arq.seek(proximo * 32);
					}
					
					//Ao encontrarmos, escrevemos o registro na posição indicada pela cabeça da lista de posições vazias, e após, atualizamos aa cabeça da lista de posições vazias,
					arq.seek(cabecaListaVazia * 32);
					arq.writeInt(reg.getChave());
					arq.writeBytes(reg.getConteudo());
					arq.seek((cabecaListaVazia * 32) + 24);
					arq.writeInt((int) anterior);
					int novaCabeca = arq.readInt();
					arq.seek(arq.getFilePointer() - 4);
					arq.writeInt(-1);
					arq.seek((anterior * 32) + 28);
					arq.writeInt(cabecaListaVazia);		
					cabecaListaVazia = novaCabeca;
					tabelaDeIndices[hashRegistro]++;
					qtdDeChaves++;
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
					
				//Inserimos o novo registro em sua devida posição:
					arq.seek(hashRegistro * 32);
					arq.writeInt(reg.getChave());
					arq.writeBytes(reg.getConteudo());
					arq.seek((hashRegistro * 32) + 24);
					arq.writeInt(-1);
					arq.writeInt(-1);
					tabelaDeIndices[hashRegistro]++;
					qtdDeChaves++;
					
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
		
		if(tabelaDeIndices[hash] == 0) {
			System.out.println("chave nao encontrada: " + chave);
			return;
		}
		
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
		
		if(tabelaDeIndices[hash] == 0) {
			System.out.println("chave nao encontrada: " + chaveParaRemover);
			return;
		}
		
		try {
			arq.seek(hash * 32);
			
			while(true) {
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
					arq.seek(arq.getFilePointer() - 32);
					long posicaoAtual = arq.getFilePointer() / 32;
					arq.writeInt(-1);
					arq.writeBytes("                    ");
					tabelaDeIndices[hash]--;
					qtdDeChaves--;
					arq.seek((cabecaListaVazia * 32) + 24);
					
					//Inserimos a nova posicao vazia na lista de posicoes vazias.
					int antListaVazia = arq.readInt();
					int proxListaVazia = arq.readInt();
					arq.seek((posicaoAtual * 32) + 24);
					arq.writeInt(antListaVazia);
					arq.writeInt(proxListaVazia);
					
					//Caso essa posição esteja antes da cabeça da lista de posições vazias, tornamos ela a nova cabeça.
					if(posicaoAtual < cabecaListaVazia) {
						arq.seek((cabecaListaVazia * 32) + 24);
						arq.writeInt((int) posicaoAtual);
						arq.seek((posicaoAtual * 32) + 28);
						arq.writeInt(cabecaListaVazia);
						cabecaListaVazia = (int) posicaoAtual;
					}

					//Caso estejamos removendo uma chave que está no meio de uma lista de chaves.
					if(ponteiroAnterior != -1 && proximoPonteiro != -1) {
						arq.seek((ponteiroAnterior * 32) + 28);
						arq.writeInt(proximoPonteiro);
						arq.seek((proximoPonteiro * 32) + 24);
						arq.writeInt(ponteiroAnterior);
						return;
					} else if(ponteiroAnterior != -1) { //Caso a chave removida seja a última da lista.
						arq.seek((ponteiroAnterior * 32) + 28);
						arq.writeInt(-1);
						arq.seek(posicaoAtual * 32); 
						return;
					} else if(proximoPonteiro != -1) { //Caso estejamos removendo um elemento que esteja na primeira posição de uma lista.
						//Como estamos removendo o primeiro elemento da lista, devemos arrastar o segundo elemento para a sua posicao
						//No proximo bloco de codigo, estamos copiando o conteudo da segunda posicao para transferirmos mais tarde.
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
					
						//Caso tenhamos uma cadeia de tamanho > 2, temos que atualizar o ponteiro anterior do terceiro elemento.
						if(proxPonteiroParaMover != -1) {
							arq.seek((proxPonteiroParaMover * 32) + 24);
							arq.writeInt(hash);		
						}
						
						//Este bloco de código move o conteúdo que copiamos do segundo registro da cadeia para a posição antes ocupada pelo primeiro elemento.
						arq.seek((hash * 32));
						arq.writeInt(chaveParaMover);
						arq.writeBytes(conteudoParaMover);
						arq.seek((hash * 32) + 24);
						arq.writeInt(-1);
						arq.writeInt(proxPonteiroParaMover);
												
						return;
					} else  { //Caso estejamos removendo o único elemento de uma lista, simplesmente retornamos, pois ele já foi apagado nas linhas 356 e 357
						return;
					}
				}
				
				arq.seek(arq.getFilePointer() + 24);
				proximo = arq.readInt();
				
				if(proximo == -1) {
					break;
				}
				
				arq.seek(proximo * 32);
			}
			
			System.out.println("chave nao encontrada: " + chaveParaRemover);
		} catch(IOException ex) {
			System.out.println("Erro ao remover registro.");
			ex.printStackTrace();
		}
	}
	
	//Para propositos de formatação, estou retornando a media de acessos como uma string.
	public String calculaMedia() {
		if(qtdDeChaves == 0) {
			return "0";
		}
		
		DecimalFormat formatador = new DecimalFormat("#.#");
		double somatorio = 0;
		double mediaDeAcessos;
		
		for(int i = 0; i < tabelaDeIndices.length; i++) {
			if(tabelaDeIndices[i] > 0) {
				somatorio += somatorio(tabelaDeIndices[i]);
			}
		}
		
		mediaDeAcessos = somatorio / qtdDeChaves;
		
		return formatador.format(mediaDeAcessos);
	}

	//Função auxiliar, que realiza o somatório de um dado n até 0. Essa função serve para auxiliar no cálculo da média de acessos.
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
