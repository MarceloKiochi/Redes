import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Aquecedor{
	private InetSocketAddress hostAddress = null;	//endereco IP local host
	private SocketChannel client = null;	//socket
	private boolean statusRegistro;		// status que verifica se o aquecedor foi identificado pelo gerenciador
	private String header;	//header da mensagem
	private String idEquipamento = "4";	//id do aquecedor

	/* Inicializa a comunicacao do aquecedor com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informe que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public Aquecedor() throws IOException {
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);	//cria um host de acesso
		this.client = SocketChannel.open(hostAddress);	//conexao com o IP e a porta
		this.client.configureBlocking(false);	//socket configurado como nao-bloqueante
		this.statusRegistro = false;
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";	//header de mensagem de identificacao
		client.write(ByteBuffer.wrap((header + idEquipamento).getBytes()));//Manda a mensagem de Identificacao: header + id
		
		ByteBuffer newBuff = ByteBuffer.allocate(256);
		int bytesRead;
		byte[] msgGerenciador;
		try {
			System.out.println("Aguardando mensagem do Gerenciador..");
			//loop para aguardar mensagem do gerenciador
			do {
				bytesRead = client.read(newBuff);	//recebe mensagem e salva em newBuff
			}while(bytesRead <= 0);	//repete enquanto nao recebe nada
			msgGerenciador = newBuff.array();	//coloca a mensagem num array de bytes
			
			if(bytesRead == 1 && msgGerenciador[0] == '2') {	//se for a mensagem de confirmacao de identificacao
				System.out.println("Aquecedor foi identificado pelo servidor ");
				this.statusRegistro = true;
			}else if(bytesRead == 2) {
				/* Se for um equipamento que foi cadastrado antes no Gerenciador e for conectado
				 * Novamente (desligo o processo e religo), pode ocorrer de a mensagem no canal vir muito rapido 
				 * E ser interpretado como uma unica mensagem(registro + sinal de ligar equipamento), assim o comando de ativacao do equipamento eh tratado nessa etapa,
				 * Pois o gerenciador soh informa uma vez para ligar o equipamento(como eh tcp, ele tem certeza que chegou a msg)*/
				if(msgGerenciador[0] == '2') {/*Identificacao*/
					System.out.println("Aquecedor foi identificado pelo servidor ");
					this.statusRegistro = true;
				}
				
				if(this.statusRegistro == true && msgGerenciador[1] == '5') {//Comando de desativacao do equipamento
					System.out.println("Aquecedor desativado!");
					Temperatura.setContribuicaoAquecedor(0);	//desliga o aquecedor (metodo explicado na classe temperatura)
				}else if(this.statusRegistro == true && msgGerenciador[1] == '4') {//Comando de ativacao do equipamento
					System.out.println("Aquecedor ativado!");
					Temperatura.setContribuicaoAquecedor(2);	//liga o aquecedor
				}
			}else {
				throw new RuntimeException("Problema de registro com o servidor");
			}
		}catch(Exception e) {
			throw new RuntimeException("Problema no registro do equipamento");
		}
	}

	public SocketChannel getClient() {
		return client;
	}

	//aguarda comandos do gerenciador para ligar ou desligar
	public void communicate(){
		int bytesRead = 0;
		byte[] msgGerenciador;
		while(client.isConnected()) {/*Enquanto a conexao nao estiver fechada*/
			ByteBuffer newBuff = ByteBuffer.allocate(256);
			try {
				System.out.println("Aguardando mensagem do Gerenciador..");
				
				// aguarda mensagem do gerenciador
				do {
					bytesRead = client.read(newBuff);
				}while(bytesRead <= 0);
				msgGerenciador = newBuff.array();
				
				if(this.statusRegistro == true && msgGerenciador[0] == '5') {//Comando de desativacao do equipamento
					System.out.println("Aquecedor desativado!");
					Temperatura.setContribuicaoAquecedor(0);	//desliga o aquecedor
				}else if(this.statusRegistro == true && msgGerenciador[0] == '4') {//comando de ativacao do equipamento
					System.out.println("Aquecedor ativado!");
					Temperatura.setContribuicaoAquecedor(2);	//liga o aquecedor
				}
			} catch (IOException e) {
				System.out.println("Servidor foi desconectado, desligando equipamento!");
				return;
			}
		}
	}

	public static void main(String[] argc) throws UnknownHostException, IOException, InterruptedException{
		Aquecedor atuador = null;
		try{
			atuador = new Aquecedor();
			atuador.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com o Gerenciador!");
		}
	}
}