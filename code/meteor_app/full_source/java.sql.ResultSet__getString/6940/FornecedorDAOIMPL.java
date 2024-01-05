package RESTAURANTE.DAO.IMPL;

import RESTAURANTE.DAO.FornecedorDAO;
import RESTAURANTE.DAO.PessoaDAO;
import RESTAURANTE.DAO.UTIL.Conexao;
import RESTAURANTE.MODEL.Fornecedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FornecedorDAOIMPL implements FornecedorDAO {

    @Override
    public void inserir(Fornecedor fornecedor) {


        Connection con = new Conexao().criarConexao();
        String sql = "insert into fornecedor value(?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement stmt = con.prepareStatement(sql);

            stmt.setInt(1, fornecedor.getCodigo());
            stmt.setString(2, fornecedor.getRazaoSocial());
            stmt.setString(3, fornecedor.getCnpj());
            stmt.setString(4, fornecedor.getInscEstadual());
            if (fornecedor.getDataFuncacao() != null) {  
                    stmt.setDate(5, new java.sql.Date(fornecedor.getDataFuncacao().getTime()));
                } else {  
                 stmt.setNull(5, Types.DATE);  
            }
            stmt.setDate(6, new java.sql.Date(fornecedor.getDataCadastro().getTime()));
            stmt.setInt(7, fornecedor.getPessoa().getCodigo());

            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void alterar(Fornecedor fornecedor) {
        Connection con = new Conexao().criarConexao();
        String sql = "update fornecedor set razaoSocial = ?, cnpj = ?, inscEstadual = ?, dataFundacao = ?,"
                + "dataCadastro = ?, pessoa_codigo = ?"
                + " where codigo = ?";
        try {
            PreparedStatement stmt = con.prepareStatement(sql);

            stmt.setString(1, fornecedor.getRazaoSocial());
            stmt.setString(2, fornecedor.getCnpj());
            stmt.setString(3, fornecedor.getInscEstadual());
            if (fornecedor.getDataFuncacao() != null) {  
                    stmt.setDate(4, new java.sql.Date(fornecedor.getDataFuncacao().getTime()));
                } else {  
                 stmt.setNull(4, Types.DATE);  
            }
            stmt.setDate(5, new java.sql.Date(fornecedor.getDataCadastro().getTime()));
            stmt.setInt(6, fornecedor.getPessoa().getCodigo());
            stmt.setInt(7, fornecedor.getCodigo());

            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void remover(Fornecedor fornecedor) {
        Connection con = new Conexao().criarConexao();
        String sql = "delete from fornecedor"
                + " where codigo = ?";
        try {
            PreparedStatement stmt = con.prepareStatement(sql);

            stmt.setInt(1, fornecedor.getCodigo());
            stmt.executeUpdate();
        } catch (SQLException ex) {
        }
    }

    @Override
    public Fornecedor buscaPorId(Integer codigo) {
        Fornecedor fornecedor = null;
        PessoaDAO pessoaDao = new PessoaDAOIMPL();
        Connection con = new Conexao().criarConexao();
        String sql = "select codigo, razaoSocial, cnpj, inscEstadual,"
                + "dataFundacao, dataCadastro, pessoa_codigo"
                + " from fornecedor where codigo = ?";
        try {
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setLong(1, codigo);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                fornecedor = new Fornecedor();
                fornecedor.setCodigo(rs.getInt("codigo"));
                fornecedor.setRazaoSocial(rs.getString("razaoSocial"));
                fornecedor.setCnpj(rs.getString("cnpj"));
                fornecedor.setInscEstadual(rs.getString("inscEstadual"));
                fornecedor.setDataFuncacao(rs.getDate("dataFundacao"));
                fornecedor.setDataCadastro(rs.getDate("dataCadastro"));
                fornecedor.setPessoa(pessoaDao.buscarPorCodigo(rs.getInt("pessoa_codigo")));
            }

        } catch (SQLException ex) {
        }
        return fornecedor;
    }

    @Override
    public Integer buscaIdMaio() {
        Integer idmaior = null;
        Connection con = new Conexao().criarConexao();
        String sql = "select max(codigo) as codigo from fornecedor";
        PreparedStatement stmt;
        try {
            stmt = con.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            idmaior = rs.getInt("codigo");

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ColaboradorDAOIMPL.class.getName()).log(Level.SEVERE, null, ex);
        }

        return idmaior;
    }

    @Override
    public List<Fornecedor> buscarTodos() {
        List<Fornecedor> fornecedores = new ArrayList<Fornecedor>();
        PessoaDAO pessoaDao = new PessoaDAOIMPL();

        Connection con = new Conexao().criarConexao();
        String sql = "select codigo, razaoSocial, cnpj, inscEstadual,"
                + "dataFundacao, dataCadastro, pessoa_codigo from fornecedor";

        try {
            PreparedStatement stmt = con.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Fornecedor fornecedor = new Fornecedor();
                fornecedor.setCodigo(rs.getInt("codigo"));
                fornecedor.setRazaoSocial(rs.getString("razaoSocial"));
                fornecedor.setCnpj(rs.getString("cnpj"));
                fornecedor.setInscEstadual(rs.getString("inscEstadual"));
                fornecedor.setDataFuncacao(rs.getDate("dataFundacao"));
                fornecedor.setDataCadastro(rs.getDate("dataCadastro"));
                fornecedor.setPessoa(pessoaDao.buscarPorCodigo(rs.getInt("pessoa_codigo")));

                fornecedores.add(fornecedor);
            }

        } catch (SQLException ex) {
        }
        return fornecedores;
    }

    @Override
    public List<Fornecedor> buscarPorNome(String razaoSocial) {
        List<Fornecedor> fornecedores = new ArrayList<Fornecedor>();
        PessoaDAO pessoaDao = new PessoaDAOIMPL();
        Connection con = new Conexao().criarConexao();
        String sql = "select * from fornecedor where razaoSocial like ? ";

        try {
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, "%" + razaoSocial + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Fornecedor fornecedor = new Fornecedor();
                fornecedor.setCodigo(rs.getInt("codigo"));
                fornecedor.setRazaoSocial(rs.getString("razaoSocial"));
                fornecedor.setCnpj(rs.getString("cnpj"));
                fornecedor.setInscEstadual(rs.getString("inscEstadual"));
                fornecedor.setDataFuncacao(rs.getDate("dataFundacao"));
                fornecedor.setDataCadastro(rs.getDate("dataCadastro"));
                fornecedor.setPessoa(pessoaDao.buscarPorCodigo(rs.getInt("pessoa_codigo")));
                fornecedores.add(fornecedor);
            }

        } catch (SQLException ex) {
        }
        return fornecedores;
    }
}
