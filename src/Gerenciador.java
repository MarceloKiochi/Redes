import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Gerenciador{
	private static SocketChannel sensorTemperatura = null;
	private static SocketChannel sensorUmidade = null;
	private static SocketChannel sensorCO2 = null;
	private static SocketChannel aquecedor = null;
	private static SocketChannel resfriador = null;
	private static SocketChannel irrigador = null;
	private static SocketChannel injetorCO2 = null;
	private static SocketChannel cliente = null;
	
	private static Integer temperaturaLida = 0;
	private static Integer limiarSupTemperatura;
	private static Integer limiarInfTemperatura;
	private static boolean statusAquecedor;
	private static boolean statusResfriador;
	
	private static Integer umidadeSoloLida = 1;
	private static Integer limiarSupUmidade;
	private static Integer limiarInfUmidade;
	private static boolean statusIrrigador;
	
	private static Integer co2Lido = 2;
	private static Integer limiarSupCO2;
	private static Integer limiarInfCO2;
	private static boolean statusInjetorCO2;
	
	private static ByteBuffer msgCliente;

	// Simuladores
	private static Temperatura ambiente = null;
	private static UmidadeSolo solo = null;
	private static CO2 ar = null;
	
	// Cada endereço remoto esta associado a um equipamento, 
	// assim quando um canal pedir uma msg vou identifica-lo pelo endereço remoto
	static Map<SocketAddress, Integer> equipaments = null;
	
	// Converte um inteiro para um vetor de bytes com seu valor binario
	private static byte[] intToByte(int inteiro) {
		int aux = inteiro;
		byte[] seqNumero = new byte[4];
		for(int i = 0; i < 4; i++) {
			seqNumero[i] = (byte) ((aux>>(i*8)) & (int)0xff);
		}
		
		return seqNumero;
	}
	
	// Converte um array de bytes para seu valor inteiro
	private static Integer byteToInt(int position, byte[] arr) {
		int num = 0;
		for(int i = 3; i >= 0; i--) {
			num = num<<8;
			num = num | (arr[i+position] & (int)0xff);
		}
		return num;
	}
	
	private static void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
		// Abre um socket pro canal
        SocketChannel channel = serverSocket.accept();
        // Configura o socket como nao bloqueante
        channel.configureBlocking(false);
        // Coloca um selector pra monitorar esse socket
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
	
	// Recebe dados pelo canal passado
	public static void receive(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(256);
		byte[] arr;
		int bytesReceived = 0;
		
		do {
			bytesReceived = channel.read(buffer);
		}while(bytesReceived <= 0);
		
		arr = buffer.array();		
		byte header = arr[0];
		
		
		if(header == '1'){	// Mensagem 1: Identificacao de Equipamento
			// Recupera o endereco remoto do equipamento pelo canal
			SocketAddress clientAddress  = channel.getRemoteAddress();			
			// Registra o endereco associada a esse equipamento na Map
			equipaments.put(clientAddress, arr[1]-'0');
			
			// Armazena o canal na variavel correta, de acordo com o 2o campo da mensagem
			switch(arr[1]) {
				case '1':
					System.out.println("Sensor de Temperatura Registrado!");
					sensorTemperatura = channel;
					break;
				case '2':
					System.out.println("Sensor de Umidade Registrado!");
					sensorUmidade = channel;
					break;
				case '3':
					System.out.println("Sensor de CO2 Registrado!");
					sensorCO2 = channel;
					break;
				case '4':
					System.out.println("Aquecedor Registrado!");
					aquecedor = channel;
					break;
				case '5':
					System.out.println("Resfriador Registrado!");
					resfriador = channel;
					break;
				case '6':
					System.out.println("Irrigador Registrado!");
					irrigador = channel;
					break;
				case '7':
					System.out.println("Injetor Registrado!");
					injetorCO2 = channel;
					break;
				case '8':
					System.out.println("Cliente Registrado!");
					cliente = channel;
					break;
			}
			
			// Responde com a mensagem 2 (confirmacao) pelo canal
			String answer = "2";
			buffer = ByteBuffer.wrap(answer.getBytes());
	        channel.write(buffer);	        
		} else if(header == '3'){	// Mensagem 3: Leitura dos sensores
			// Identifica o sensor
			SocketAddress clientAddress  = channel.getRemoteAddress(); // Recupera o endereco remoto do equipamento
			Integer id = equipaments.get(clientAddress);	// Recupera o id do equipamento
			
			// Armazena o valor lido na variavel correspondente ao sensor
			switch(id) {
				case 1:
					temperaturaLida = byteToInt(2, arr);
					break;
				case 2:
					umidadeSoloLida = byteToInt(2, arr);
					break;
				case 3:					
					co2Lido = byteToInt(2, arr);
					break;
			}
		} else if(header == '6') {	// Mensagem 6: Pedido de configuracao de limiares pelo cliente
			// Identifica os valores dos campos da mensagem
			char tipoParametro = (char)arr[1];
			int minVal = byteToInt(2, arr);
			int maxVal = byteToInt(6, arr);
		
			String printString = "Novos limiares de ";
			
			if(tipoParametro == '1') {
				printString += "temperatura: ";
				limiarInfTemperatura = minVal;
				limiarSupTemperatura = maxVal;
			} else if(tipoParametro == '2') {
				printString += "umidade: ";
				limiarInfUmidade = minVal;
				limiarSupUmidade = maxVal;
			} else {
				printString += "CO2: ";
				limiarInfCO2 = minVal;
				limiarSupCO2 = maxVal;
			}
			
			printString += minVal + " a " + maxVal;
			System.out.println(printString);
		} else if(header == '7') {	// Mensagem 7: Requisicao de Leitura dos Sensores
			String printString = "Enviando leitura de ";
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();	//estrutura para concatenar arrays de bytes
						
			// Identifica o tipo de sensor requisitado e monta a resposta no buffer "msgCliente"
			//que sera enviado no metodo 'send'
			char tipoParametro = (char)arr[1];
			switch(tipoParametro) {
				case '1':
					printString += "temperatura: " + temperaturaLida.toString();
					
					byteArray.write(("8" + tipoParametro).getBytes());
					byteArray.write(intToByte(temperaturaLida.intValue()));
					msgCliente = ByteBuffer.wrap(byteArray.toByteArray());
					break;
				case '2':
					printString += "umidade: " + umidadeSoloLida.toString();
					
					byteArray.write(("8" + tipoParametro).getBytes());
					byteArray.write(intToByte(umidadeSoloLida.intValue()));
					msgCliente = ByteBuffer.wrap(byteArray.toByteArray());
					break;
				case '3':
					printString += "CO2: " + co2Lido.toString();
					
					byteArray.write(("8" + tipoParametro).getBytes());
					byteArray.write(intToByte(co2Lido.intValue()));
					msgCliente = ByteBuffer.wrap(byteArray.toByteArray());
					break;	
			}
			System.out.println(printString);
		}
	}	
	
	// Envia dados pelo canal passado 
	public static void send(SelectionKey key) throws IOException {
		// Recupera o endereco remoto do equipamento pelo canal
		SocketChannel channel = (SocketChannel) key.channel();	
		
		// Registra o endereco associada a esse equipamento na Map
		SocketAddress clientAddress  = channel.getRemoteAddress();
		
		// Identifica o equipamento
		Integer idEquipaments = equipaments.get(clientAddress);
		
		// Caso o equipamento solicite a leitura mas nao tenha sido identificado
		if(idEquipaments == null) 
			return;
		
		switch(idEquipaments) {
			case 4:
				// Checa temperaturaLida e compara com os valores dos limiares
				// Se o Aquecedor estiver desligado e temperaturaLida estiver abaixo do limiar
				if(statusAquecedor == false && temperaturaLida < limiarInfTemperatura) { 
					System.out.println("Servidor informando ao aquecedor para ligar!");
					// Envia mensagem 4 ao Aquecedor (ligar)
					ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
					aquecedor.write(msg);
					statusAquecedor = true;
				} else if(statusAquecedor == true && temperaturaLida >= limiarSupTemperatura) {
					System.out.println("Servidor informando ao aquecedor para desligar!");
					// Envia mensagem 5 ao Aquecedor (desligar)
					ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
					aquecedor.write(msg);
					statusAquecedor = false;
				}
				break;
				// Similarmente para o Resfriador e para os outro atuadores
			case 5:
				//Se o Resfriador estiver desligado e temperaturaLida estiver acima do limiar
				if(statusResfriador == false && temperaturaLida > limiarSupTemperatura) {
					System.out.println("Servidor informando ao resfriador para ligar!");
					// Envia mensagem 4 ao Resfriador (ligar)
					ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
					resfriador.write(msg);
					statusResfriador = true;
				} else if(statusResfriador == true && temperaturaLida <= limiarInfTemperatura) {
					System.out.println("Servidor informando ao resfriador para desligar!");
					// Envia mensagem 5 ao Aquecedor (desligar)
					ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
					resfriador.write(msg);
					statusResfriador = false;
				}
				break;
			case 6:
				if(statusIrrigador == false && umidadeSoloLida < limiarInfUmidade) {
					System.out.println("Servidor informando ao irrigador para ligar!");
					ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
					irrigador.write(msg);
					statusIrrigador = true;
				}else if(statusIrrigador == true && umidadeSoloLida >= limiarSupUmidade) {
					System.out.println("Servidor informando ao irrigador para desligar");
					ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
					irrigador.write(msg);
					statusIrrigador = false;
				}
				
				break;
			case 7:
				if(statusInjetorCO2 == false && co2Lido < limiarInfCO2) {
					System.out.println("Servidor informando ao injetor para ligar!");
					ByteBuffer msg = ByteBuffer.wrap("4".getBytes());
					injetorCO2.write(msg);
					statusInjetorCO2 = true;
				}else if(statusInjetorCO2 == true && co2Lido >= limiarSupCO2) {
					System.out.println("Servidor informando ao injetor para desligar!");
					ByteBuffer msg = ByteBuffer.wrap("5".getBytes());
					injetorCO2.write(msg);
					statusInjetorCO2 = false;
				}
				break;
			case 8:
				// Se houver mensagem a ser enviada para o cliente no buffer e o mesmo estiver requisitando
				if(msgCliente != null) {
					cliente.write(msgCliente);
					msgCliente = null;
				}
				break;
		}
	}

	private static void setStatusDefaultEquipamentos() {
		statusAquecedor = false;
		statusResfriador = false;
		statusIrrigador = false;
		statusInjetorCO2 = false;
		
		co2Lido = 160;
		limiarSupCO2 = 170;
		limiarInfCO2 = 150;
		CO2.setContribuicaoCO2(0);
		
		umidadeSoloLida = 15;
		limiarSupUmidade = 20;
		limiarInfUmidade = 5;
		UmidadeSolo.setContribuicaoUmidadeEquip(0);
		
		temperaturaLida = 28;
		limiarSupTemperatura = 32;
		limiarInfTemperatura = 25;
		ambiente.setContribuicaoAquecedor(0);
		ambiente.setContribuicaoResfriador(0); //A contribuicao do equipamento eh inicializado com 0 pois os atuadores inicializam desligados		
	}
	
	/* Caso o equipamento seja desconectado os status do equipamento sao resetados*/
	private static void resetStatusEquip(SocketChannel equip) {
		if(equip == aquecedor) {
			System.out.println("Aquecedor foi desconectado!");
			statusAquecedor = false;
			if(statusResfriador == false)//Se o resfriador nao estiver ligado
				ambiente.setContribuicaoResfriador(0);
		}else if(equip == sensorTemperatura) {
			System.out.println("Sensor de Temperatura foi desconectado!");
		}else if(equip == resfriador) {
			System.out.println("Resfriador foi desconectado!");
			statusResfriador = false;
			if(statusAquecedor == false)//Se o aquecedor nao estiver ligado
				ambiente.setContribuicaoAquecedor(0);
		}else if(equip == sensorUmidade) {
			System.out.println("Sensor de Umidade foi desconectado!");
		}else if(equip == irrigador) {
			System.out.println("Irrigador foi desconectado!");
			statusIrrigador = false;
			UmidadeSolo.setContribuicaoUmidadeEquip(0);
		}else if(equip == injetorCO2) {
			statusInjetorCO2 = false;
			System.out.println("Injetor foi desconectado!");
			CO2.setContribuicaoCO2(0);
		}else if(equip == sensorCO2) {
			System.out.println("Sensor de CO2 foi desconectado!");
		}else if(equip == cliente) {
			System.out.println("Cliente foi desconectado!");
		}
	}
	
	public static void main(String[] argc) throws IOException{
		Selector selector  = Selector.open();
		ServerSocketChannel serverSocket = ServerSocketChannel.open();
		
		InetSocketAddress hostAddress = new InetSocketAddress("127.0.0.1", 9545);// ip localhost e porta qualquer
		try {
			serverSocket.bind(hostAddress);
		}catch(Exception e) {/*Se ja tiver um gerenciador em execucao ou ip e porta tiver sendo usada*/
			System.out.println("O localhost com a porta 9545 ja esta em uso!");
			return;
		}
		serverSocket.configureBlocking(false);
		
		serverSocket.register(selector, SelectionKey.OP_ACCEPT);// Coloca o selector para administrar a escuta dos canais
		
		equipaments = new HashMap<SocketAddress, Integer>();		
		
		// Inicializa a simulacao da variacao dos parametros
		ambiente = new Temperatura();
		ambiente.start();
		solo = new UmidadeSolo();
		solo.start();
		ar = new CO2();
		ar.start();
		
		setStatusDefaultEquipamentos();
		System.out.println("Gerenciador iniciado!");
		
		Set<SelectionKey> selectedKeys;
		while(true) {
			// Recupera a lista de equipamentos que fizeram algo no canal
			selector.select();
			selectedKeys = selector.selectedKeys();
			
			for(Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext();) {
				SelectionKey key = it.next();
				
				if(key.isAcceptable()) { // Canal que deseja se registrar no servidor
					try {
						register(selector, serverSocket);
					}catch(Exception e) {
						System.out.println("Problema no registro do socket do cliente");
					}
				}
				
				if(key.isValid() && key.isReadable()) { // Algum equipamento se comunicou
					try{
						// Le e trata a mensagem
						receive(key);
					}catch(Exception e) {						
						/* Se o Equipamento desligar e for captado algo no canal
						 * Vai acontecer problema de leitura, aqui trato a desconexao com o canal do equipamento*/
						SocketChannel equip = (SocketChannel) key.channel();
						resetStatusEquip(equip);
						equip.close();
					}
				}
				
				if(key.isValid() && key.isWritable()) {
					try {
						send(key);
					}catch(Exception e) {
						/* Se o Equipamento desligar antes de receber os dados no canal
						 * Vai acontecer problema de envio, aqui trato a desconexao do canal do equipamento*/
						SocketChannel equip = (SocketChannel) key.channel();
						resetStatusEquip(equip);
						equip.close();
					}
				}
				it.remove();
			}
		}
	}
}