package com.luckybidder.server;

import com.luckybidder.client.LuckyBidderService;

import java.io.File;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.luckybidder.shared.*;
/**
 * The server-side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class LuckyBidderImpl extends RemoteServiceServlet implements LuckyBidderService {
	
	
	DB dbUtenti;
	DB dbProdotti;
	DB dbDomande;
	DB dbOfferte;
	DB dbRisposte;
	DB dbCategorie;
	
	public Utente loginUtente(String username, String password) {
		dbUtenti = getDBUtenti();
		BTreeMap<String, Utente> mapUtenti = dbUtenti.getTreeMap("MapUtenti");
		if(mapUtenti.containsKey(username)) {
			if(mapUtenti.get(username).getPassword().equals(password)) {
				return mapUtenti.get(username);
			}
		} else if( username.equals("admin") && password.equals("admin")) {
			Utente adminUtente = new Utente();
			adminUtente.setUsername("admin");
			return adminUtente;
		}
		return null;
		
	}
	
	public boolean registraUtente(Utente utente) {
		
		dbUtenti = getDBUtenti();
		BTreeMap<String, Utente> mapUtenti = dbUtenti.getTreeMap("MapUtenti");
		
		if(!mapUtenti.containsKey(utente.getUsername()) && !utente.getUsername().equals("admin") && !utente.getUsername().equals("Admin") ) {
			mapUtenti.put(utente.getUsername(), utente);
			dbUtenti.commit();
			dbUtenti.close();
			System.out.println("Registrato Utente: " + utente.toString());
			return true;
		} else {
			dbUtenti.close();
			System.out.print("Username " + utente.getUsername() +" gi� esistente");

			return false;
		}
	}
	
	public boolean vendiProdotto(Prodotto prodotto) {
			
			dbProdotti = getDBProdotti();
			BTreeMap<Integer, Prodotto> mapProdotti = dbProdotti.getTreeMap("MapProdotti");
			int size = mapProdotti.size();
			int id = size + 1;
			prodotto.setIdProdotto(id);
			mapProdotti.put(id, prodotto);
			dbProdotti.commit();
			dbProdotti.close();
			System.out.println("Prodotto messo in vendita: " + prodotto.toString());
			return true;
	}
	
	@Override
	public Prodotto getProdottoSingolo(int id) {
		dbProdotti = getDBProdotti();
		BTreeMap<Integer, Prodotto> mapProdotti = dbProdotti.getTreeMap("MapProdotti");
		Prodotto prodottoEstratto = new Prodotto();
		if(!mapProdotti.isEmpty()) {
			prodottoEstratto = mapProdotti.get(id);
		}
		return prodottoEstratto;
	}
	
	@Override
	public ArrayList<Prodotto> getProdotti() {
	
		dbProdotti = getDBProdotti();
	
		BTreeMap<Integer, Prodotto> mapProdotti = dbProdotti.getTreeMap("MapProdotti");
		ArrayList<Prodotto> listaProdotti = new ArrayList<Prodotto>();
		if(!mapProdotti.isEmpty()){
			for(Map.Entry<Integer, Prodotto> prodotto : mapProdotti.entrySet()){
				
				Prodotto prodottoEstratto = new Prodotto();
				
				Date controllaData = null;
				prodottoEstratto = prodotto.getValue();
				controllaData = prodottoEstratto.getDataScadenza();
				
				Date dataOggi = Calendar.getInstance().getTime();
				if((controllaData.compareTo(dataOggi)<0)&& (prodottoEstratto.getStato().contentEquals("APERTA"))) {
					modificaScadenza(prodottoEstratto, prodottoEstratto.getIdProdotto());
				}
				if(!prodottoEstratto.getStato().equals("CHIUSA")) {
					listaProdotti.add(prodottoEstratto);
				}
			}
		}
		//La lista viene ordinata in ordine di scadenza
		Collections.sort(listaProdotti);
		return listaProdotti;
	}
	
	@Override
	public boolean modificaScadenza(Prodotto prodotto, int id) {
	
		dbProdotti = getDBProdotti();
		BTreeMap<Integer, Prodotto> mapProdotti = dbProdotti.getTreeMap("MapProdotti");
	
		dbOfferte = getDBOfferte(); 
		BTreeMap<Integer, Offerta> mapOfferte = dbOfferte.getTreeMap("MapOfferte");
	
		Prodotto prodottoModificato = new Prodotto();
		if(!mapProdotti.isEmpty()){
			prodottoModificato = prodotto;
			Offerta offertaMax = getMaxOfferta(prodottoModificato.getIdProdotto());	//Si cerca l'offerta massima per l'oggetto
			if (offertaMax.getPrezzo()>0) {
				//Se � presente un offerta, si setta come vincitore l'username
				//dell'utente che ha presentato l'offerta pi� alta
				prodottoModificato.setVincitore(offertaMax.getUsername()); 
			}
			//L'asta viene chiusa
			prodottoModificato.setStato("CHIUSA");	
			//Si aggiorna il valore dell'oggetto
			mapProdotti.replace(id, prodottoModificato);	
			dbProdotti.commit();
			return true;
		}
		else{
			return false;
		}
	}
	
	@Override
	public Offerta getMaxOfferta(int idProdotto) {
		dbOfferte = getDBOfferte();
		BTreeMap<Integer, Offerta> mapOfferte = dbOfferte.getTreeMap("MapOfferte");
		Offerta offertaMax = new Offerta();
		double prezzoMax = 0;
		if(!mapOfferte.isEmpty()) {
			for(Map.Entry<Integer, Offerta> offerta : mapOfferte.entrySet()){
				//Se l'offerta � riferita all'oggetto, non � stata eliminata ed � maggiore dell' ultimo prezzo massimo
				if (offerta.getValue().getIdProdotto() == idProdotto && offerta.getValue().getPrezzo()>prezzoMax && !(offerta.getValue().getIdProdotto()==-1)){
					offertaMax = offerta.getValue();
					//Si prende come offerta massima
					prezzoMax=offertaMax.getPrezzo();
				}
			}	
		}
		return offertaMax;
	}
	
	@Override
	public boolean offri(Offerta offerta) {
		/*int id;
		int idOggetto = offerta.getIdProdotto();
		String username = offerta.getUsername();
		double prezzo = offerta.getPrezzo();	
		Date date = offerta.getDataOfferta();*/
		dbOfferte = getDBOfferte();
		BTreeMap<Integer, Offerta> mapOfferte = dbOfferte.getTreeMap("MapOfferte");
			int id=(mapOfferte.size()+1);
			offerta.setIdOfferta(id);
			//Offerta nuovaOfferta = new Offerta(id,idOggetto,username,prezzo,date);
			mapOfferte.put(id, offerta);
			dbOfferte.commit();
			System.out.println("Inserimento offerta: "+ offerta.toString());
			return true;
	}
	
	@Override
	public ArrayList<Offerta> getOfferte(int idProdotto){
		
		dbOfferte=getDBOfferte();
		BTreeMap<Integer,Offerta> mapOfferte = dbOfferte.getTreeMap("MapOfferte");
		ArrayList<Offerta> listaOfferte = new ArrayList<Offerta>();
		if(!mapOfferte.isEmpty()) {
			for(Map.Entry<Integer, Offerta> offerta : mapOfferte.entrySet()) {
				if(offerta.getValue().getIdProdotto() == idProdotto && !(offerta.getValue().getIdProdotto()==-1)) {
					Offerta offertaEstratta = new Offerta();
					offertaEstratta = offerta.getValue();
					listaOfferte.add(offertaEstratta);
				}
			}
		}
		return listaOfferte;
	}


	private DB getDBUtenti() {

		dbUtenti = DBMaker.newFileDB(new File("MapDBUtenti")).closeOnJvmShutdown().make();		
		return dbUtenti;	
	}
	
	private DB getDBOfferte() {

		dbOfferte = DBMaker.newFileDB(new File("MapDBOfferte")).closeOnJvmShutdown().make();		
		return dbOfferte;	
	}
	
	private DB getDBProdotti() {

		dbProdotti = DBMaker.newFileDB(new File("MapDBProdotti")).closeOnJvmShutdown().make();		
		return dbProdotti;	
	}
	
	//prendo i prodotti messi in vendita per ogni utente
	public ArrayList<Prodotto> getProdottiVenduti(String username) {
		
		dbProdotti = getDBProdotti();
		BTreeMap<Integer, Prodotto> mapProdotti = dbProdotti.getTreeMap("MapProdotti");
		Prodotto prodottoEstratto = new Prodotto();
		ArrayList<Prodotto> listaProdotti= new ArrayList<Prodotto>();
		if(!mapProdotti.isEmpty()) {
			for(Map.Entry<Integer, Prodotto> prodotto : mapProdotti.entrySet()) {
				if(prodotto.getValue().getUsername().equals(username) && !(prodotto.getValue().getIdProdotto()==-1)) {
					prodottoEstratto = prodotto.getValue();
					listaProdotti.add(prodottoEstratto);
				}
			}
			
		}
		return listaProdotti;
	}
	
	//prendo le offerte fatte per ogni utente
		public ArrayList<Offerta> getOfferteFatte(String username) {
			
			dbOfferte = getDBOfferte();
			
			BTreeMap<Integer, Offerta> mapOfferte = dbOfferte.getTreeMap("MapOfferte");
			Offerta offertaFatta = new Offerta();
			ArrayList<Offerta> listaOfferte= new ArrayList<Offerta>();
			if(!mapOfferte.isEmpty()) {
				for(Map.Entry<Integer, Offerta> offerta : mapOfferte.entrySet()){
					if(offerta.getValue().getUsername().equals(username) && !(offerta.getValue().getIdProdotto()==-1)) {
						offertaFatta = offerta.getValue();
						listaOfferte.add(offertaFatta);
					}
				}
				
			}
			return listaOfferte;
		}
	
	/**
	 * Escape an html string. Escaping data received from the client helps to
	 * prevent cross-site script vulnerabilities.
	 * 
	 * @param html the html string to escape
	 * @return the escaped string
	 */
	private String escapeHtml(String html) {
		if (html == null) {
			return null;
		}
		return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

	
	private DB getDBCategorie() {

		dbCategorie = DBMaker.newFileDB(new File("MapDBCategorie")).closeOnJvmShutdown().make();		
		return dbCategorie;	
	}

	public boolean aggiungiCategoria(Categoria categoria, String padre) {
		dbCategorie = getDBCategorie();
		BTreeMap<Integer, Categoria> mapCategorie = dbCategorie.getTreeMap("MapDBCategorie");
		boolean aggiungi= true;
		
		if(!mapCategorie.isEmpty()) {
			for(Map.Entry<Integer, Categoria> categorie : mapCategorie.entrySet()){	
				if (categorie.getValue().getNomeCategoria().equals(categoria.getNomeCategoria())) {
					aggiungi=false;
					System.out.println("Categorie gia esistente");
					break;
				}
			}
		}
		
		if (aggiungi) {
			System.out.println("Devo inserire la categoria");
			int id = (mapCategorie.size()+1);
			boolean catPadreCheck = false;
			Categoria catPadre = null;
			categoria.setId(id);
			
			if(padre==null) {
				System.out.println("Padre null");
				catPadre = new Categoria("null");
				catPadreCheck = true;
			} else {
				System.out.println("Cerco padre...");
				if(!mapCategorie.isEmpty()) {
					for(Map.Entry<Integer, Categoria> categorie: mapCategorie.entrySet()) {
						if(categorie.getValue().getNomeCategoria().equals(padre)) {
							catPadreCheck = true;
							System.out.println("Padre trovato");
							catPadre = categorie.getValue();
							System.out.println(catPadre.toString()+"\n");
						}
					}
				}
			}
			if(catPadreCheck) {
				categoria.setPadre(catPadre);
				mapCategorie.put(id,categoria);
				if(catPadre != null) {
					System.out.println("Padre != null");
					for(Map.Entry<Integer, Categoria> categorie: mapCategorie.entrySet()) {
						if(categorie.getValue().getNomeCategoria().equals(padre)) {
							System.out.println("Padre trovato!!");
							Categoria p = categorie.getValue();
							p.setCategoriaFiglia(categoria);
							
							System.out.println(p.toString()+"\n");
							mapCategorie.remove(p.getId());
							mapCategorie.put(p.getId(), p);
							
						}
					}
				}
			} else {
				dbCategorie.commit();
				System.out.println("Check padre esistente false");
				return false;
			}
			dbCategorie.commit();
			System.out.println("Aggiunta Categoria : " + categoria.toString());
			return true; 	
		}
		else {
			dbCategorie.commit();
			System.out.println("Aggiungi false");
			return false;
		}
			
	}
	
	public Categoria getCategoria(int idCategoria) {
		dbCategorie = getDBCategorie();
		BTreeMap<Integer, Categoria> mapCategorie = dbCategorie.getTreeMap("MapDBCategorie");
		Categoria categoria = mapCategorie.getOrDefault(idCategoria, null);
		return categoria;
	}
	
	public ArrayList<Categoria> getAllCategorie(){
		dbCategorie = getDBCategorie();
		BTreeMap<Integer, Categoria> mapCategorie = dbCategorie.getTreeMap("MapDBCategorie");
		
		ArrayList<Categoria> listCategorie = new ArrayList<Categoria>();
		Categoria root = new Categoria("ROOT");
		if(!mapCategorie.isEmpty()) {
			for(Map.Entry<Integer, Categoria> categoria : mapCategorie.entrySet()) {
				listCategorie.add(categoria.getValue());		
			}
		}
		//System.out.println(root.toString());
		return listCategorie;		
	}
	
	public boolean modificaCategoria(String nomeCategoria, String nomeNuovo) {
		dbCategorie = getDBCategorie();
		BTreeMap<Integer, Categoria> mapCategorie = dbCategorie.getTreeMap("MapDBCategorie");
		boolean result = false;
		if(!mapCategorie.isEmpty()) {
			for(Map.Entry<Integer, Categoria> categoria : mapCategorie.entrySet()) {
				if(categoria.getValue().getNomeCategoria().contentEquals(nomeCategoria)) {
					Categoria temp = categoria.getValue();
					temp.setNomeCategoria(nomeNuovo);;
					mapCategorie.remove(temp.getId());
					mapCategorie.put(temp.getId(), temp);
					result = true;
					
				}
			}
		}
		dbCategorie.commit();
		return result;		
	}

	private DB getDBDomande() {
		dbDomande = DBMaker.newFileDB(new File("MapDBDomande")).closeOnJvmShutdown().make();		
		return dbDomande;
	}
	
	@Override
	public Domanda getDomanda(String username, int id) {
		dbDomande = getDBDomande();
		BTreeMap<Integer,Domanda> mapDomande = dbDomande.getTreeMap("MapDBDomande");
		Domanda domandaReturn = null;
		if(!mapDomande.isEmpty()) {
			for(Map.Entry<Integer, Domanda> domanda : mapDomande.entrySet()) {
				if(domanda.getValue().getIdProdotto()==id && domanda.getValue().getDaCheutente().equals(username)) {
					domandaReturn = domanda.getValue();
				}
			}
		}
		dbDomande.commit();
		return domandaReturn;
	}

	@Override
	public boolean mandaDomanda(String nomeProdotto, String testoDomanda, String username, int id, String usernameVenditore) {
		dbDomande = getDBDomande();
		BTreeMap<Integer,Domanda> mapDomande = dbDomande.getTreeMap("MapDBDomande");
		Domanda domanda = new Domanda();
		domanda.setDaCheutente(username);
		domanda.setIdProdotto(id);
		domanda.setTestoDomanda(testoDomanda);
		domanda.setNomeUtenteVenditore(usernameVenditore);
		domanda.setNomeProdotto(nomeProdotto);
		domanda.setIdDomanda(mapDomande.size()+1);
		mapDomande.put(mapDomande.size()+1, domanda);
		dbDomande.commit();
		return true;
	}

	@Override
	public Risposta getRisposta(int idDomanda) {
		dbRisposte = getDBRisposte();
		Risposta returnRisposta = null;
		BTreeMap<Integer,Risposta> mapRisposte = dbRisposte.getTreeMap("MapDBRisposte");
		for(Map.Entry<Integer, Risposta> risposta : mapRisposte.entrySet()) {
			if(risposta.getValue().getIdDomandaRelativa() == idDomanda) {
				returnRisposta = risposta.getValue();
			}
		}
		return returnRisposta;
	}

	@Override
	public ArrayList<Domanda> getDomandeToUsername(String usernameVendProdotto) {
		dbDomande = getDBDomande();
		BTreeMap<Integer,Domanda> mapDomande = dbDomande.getTreeMap("MapDBDomande");
		
		ArrayList<Domanda> listResult = new ArrayList<Domanda>();
		if(!mapDomande.isEmpty()) {
			for(Map.Entry<Integer, Domanda> domanda : mapDomande.entrySet()) {
				if(domanda.getValue().getNomeUtenteVenditore().equals(usernameVendProdotto)) {
					System.out.println("Trovata domanda per " + usernameVendProdotto);
					listResult.add(domanda.getValue());
				}
				
			}
		}
		dbDomande.commit();
		return listResult;
	}

	
	private DB getDBRisposte() {
		dbDomande = DBMaker.newFileDB(new File("MapDBRisposte")).closeOnJvmShutdown().make();		
		return dbDomande;
	}
	
	//@Override
	public boolean eliminaProdotto(int idProdotto) {
		//System.out.println(idProdotto);
		dbProdotti = getDBProdotti();
		//dbDomande = getDBDomande();
		//dbOfferte = getDBOfferte();
		//dbRisposte = getDBRisposte();
		boolean result = false;
		BTreeMap <Integer,Prodotto> mapProdotti = dbProdotti.getTreeMap("MapDBProdotti");
		for(Map.Entry<Integer,Prodotto> prodotto : mapProdotti.entrySet()) {
			result = true;
			System.out.println(prodotto.toString());
			if(prodotto.getValue().getIdProdotto() == idProdotto) {
				System.out.println("Trovato " + prodotto.toString());
				/*Prodotto prodottoElim = new Prodotto();
				prodottoElim = prodotto.getValue();
				prodottoElim.setIdProdotto(-1);
				mapProdotti.replace(prodotto.getValue().getIdProdotto(), prodottoElim);*/
				mapProdotti.remove(prodotto.getValue().getIdProdotto());
			}
		}
		dbProdotti.commit();
		return result;
	}
		/*
		BTreeMap<Integer,Offerta> mapOfferte = dbOfferte.getTreeMap("MapDBOfferte");
		if(!mapOfferte.isEmpty()) {
			for(Map.Entry<Integer,Offerta> offerta : mapOfferte.entrySet()) {
				if(offerta.getValue().getIdProdotto() == idProdotto) {
					System.out.println("Trovata offerta " + idProdotto);
					Offerta offertaEliminata = new Offerta();
					offertaEliminata = offerta.getValue();
					offertaEliminata.setIdOfferta(-1);
					mapOfferte.replace(offerta.getValue().getId(),offertaEliminata);
					//mapOfferte.remove(offerta.getValue().getId());
				}
			}
			
		}
		dbOfferte.commit();
		BTreeMap<Integer,Domanda> mapDomande = dbDomande.getTreeMap("MapDBDomande");
		if(!mapDomande.isEmpty()) {
			for(Map.Entry<Integer, Domanda> domanda : mapDomande.entrySet()) {
				if(domanda.getValue().getIdProdotto() == idProdotto) {
					BTreeMap<Integer,Risposta> mapRisposte = dbRisposte.getTreeMap("MapDBRispsote");
					if(!mapRisposte.isEmpty()) {
						for(Map.Entry<Integer, Risposta>risposta : mapRisposte.entrySet()) {
							if(risposta.getValue().getIdDomandaRelativa() == domanda.getValue().getIdDomanda()) {
								Risposta rispostaEliminata = new Risposta();
								rispostaEliminata = risposta.getValue();
								rispostaEliminata.setIdRisposta(-1);
								//mapRisposte.remove(risposta.getValue().getIdRisposta());
								
							}
						}
						
					}
					dbRisposte.commit();
					Domanda domandaEliminata = new Domanda();
					domandaEliminata = domanda.getValue();
					domandaEliminata.setIdDomanda(-1);
					mapDomande.replace(domanda.getValue().getIdDomanda(), domandaEliminata);
					//mapDomande.remove(domanda.getValue().getIdDomanda());
				}
			}
			
		}
		dbDomande.commit();
		BTreeMap<Integer,Prodotto> mapProdotti = dbProdotti.getTreeMap("MapDBProdotti");
		if(!mapProdotti.isEmpty()) {
			for(Map.Entry<Integer,Prodotto> prodotto : mapProdotti.entrySet()) {
				if(prodotto.getValue().getIdProdotto() == idProdotto) {
					//System.out.print(prodotto.getValue().toString());
					Prodotto prodottoEliminato = new Prodotto();
					prodottoEliminato = mapProdotti.get(idProdotto);
					prodottoEliminato.setIdProdotto(-1);
					mapProdotti.replace(idProdotto, prodottoEliminato);
					//mapProdotti.remove(prodotto.getValue().getIdProdotto());
				}
			}
		}
		dbProdotti.commit();*/
	@Override
	public boolean inviaRisposta(int idDomanda, String testoRisposta) {
		dbRisposte = getDBRisposte();
		BTreeMap<Integer,Risposta> mapRisposte = dbRisposte.getTreeMap("MapDBRisposte");
		Risposta risposta = new Risposta();
		risposta.setIdDomandaRelativa(idDomanda);
		risposta.setRisposta(testoRisposta);
		risposta.setIdRisposta(mapRisposte.size()+1);
		mapRisposte.put(mapRisposte.size()+1, risposta);
		dbRisposte.commit();
		return false;
	}
}
