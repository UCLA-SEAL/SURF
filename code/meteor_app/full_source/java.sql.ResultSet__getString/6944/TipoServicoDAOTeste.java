package testes;

import static org.junit.Assert.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import model.TipoServico;

import org.junit.Test;

import dao.TipoServicoDAO;

public class TipoServicoDAOTeste {

	TipoServico tiposervico = new TipoServico();
	TipoServico tiposervico2 = new TipoServico();
	TipoServicoDAO servicoDAO = TipoServicoDAO.getInstance();
	
	@Test
	public void getInstanceDeTipoServicoDAODeveRetonarInstanciaCorrente() {
		assertEquals(TipoServicoDAO.getInstance(), servicoDAO);
	}

	@Test
	public void inserirDeTipoServicoDAODeveCadastrarUmTipoServico() {
		try {
			assertTrue(servicoDAO.incluir(tiposervico));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void excluirDeTipoServicoDAODeveEnviarUmTipoServico() {
		try {
			assertTrue(servicoDAO.excluir(tiposervico));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void alterarDeTipoServicoDAODeveEnviarUmTipoServico() {
		try {
			assertTrue(servicoDAO.alterar(tiposervico.getNomeTipoServico(),tiposervico, tiposervico2));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void inserirDeTipoServicoDAOPassandoUmServicoNulo() {
		try {
			assertFalse(servicoDAO.incluir(null));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void excluirDeTipoServicoDAOPassandoUmServicoNulo() {
		try {
			assertFalse(servicoDAO.excluir(null));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void alterarDeTipoServicoDAOPassandoUmServicoNulo() {
		try {
			assertFalse(servicoDAO.alterar(tiposervico.getNomeTipoServico(), tiposervico, null));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void alterarDeTipoServicoDAOPassandoUmServicoAlteradoNulo() {
		try {
			assertFalse(servicoDAO.alterar(tiposervico.getNomeTipoServico(), null, tiposervico));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void mostrarServicosDeTipoServicoDAODeveMostrarServico() {
		try {
			ResultSet rs = servicoDAO.mostrarTipoServicoCadastrados(tiposervico);
			
			while (rs.next()) {
				String nome = rs.getString("nome");
				assertNotNull(nome);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void pesquisarPorNomeDeTipoServicoDAODeveMostrarServico() {
		try {
			ResultSet rs = servicoDAO.pesquisarPorNome(tiposervico);
			
			while (rs.next()) {
				String nome = rs.getString("nome");
				assertNotNull(nome);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
