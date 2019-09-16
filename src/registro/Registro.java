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
		prox = ant = -1;
	}
	
	public Registro()	{ }
	
	public int hash(int TAM_ARQUIVO) {
		return chave % TAM_ARQUIVO;
	}

	public int getProx() {
		return prox;
	}

	public void setProx(int prox) {
		this.prox = prox;
	}

	public int getAnt() {
		return ant;
	}

	public void setAnt(int ant) {
		this.ant = ant;
	}

	public void setChave(int chave) {
		this.chave = chave;
	}

	public int getChave() {
		return chave;
	}

	public String getConteudo() {
		return conteudo;
	}

	public void setConteudo(String conteudo) {
		this.conteudo = conteudo;
	}	
}
