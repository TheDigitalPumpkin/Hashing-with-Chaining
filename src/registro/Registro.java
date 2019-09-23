package registro;

public class Registro 
{
	protected int chave;
	protected String conteudo;
	protected int prox;
	protected int ant;
	//Size in bytes = 32
	
	public Registro(int chave, String conteudo) {
		this.chave = chave;
		this.conteudo = conteudo;
		prox = -1;
		ant = -1;
	}
	
	public Registro()	{ }
	
	public int hash(int TAM_ARQUIVO) {
		return chave % TAM_ARQUIVO;
	}

	public int getProx() {
		return prox;
	}

	public int getAnt() {
		return ant;
	}


	public int getChave() {
		return chave;
	}

	public String getConteudo() {
		return conteudo;
	}
}
