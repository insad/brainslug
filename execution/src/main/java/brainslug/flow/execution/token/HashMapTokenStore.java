package brainslug.flow.execution.token;

import brainslug.util.IdGenerator;
import brainslug.flow.Identifier;
import brainslug.util.Option;

import java.util.*;

public class HashMapTokenStore implements TokenStore {

  Map<Identifier, List<Identifier>> flowToInstanceMap = Collections.synchronizedMap(new HashMap<Identifier, List<Identifier>>());
  Map<Identifier, List<Token>> instanceToTokenMap = new HashMap<Identifier, List<Token>>();

  IdGenerator idGenerator;

  public HashMapTokenStore(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  @Override
  public TokenList getInstanceTokens(Identifier instanceId) {
    if (instanceToTokenMap.get(instanceId) == null) {
      List<Token> instanceTokens = new ArrayList<Token>();
      instanceToTokenMap.put(instanceId, instanceTokens);
      return new TokenList(instanceTokens);
    } else {
      return new TokenList(instanceToTokenMap.get(instanceId));
    }
  }

  @Override
  public TokenList getNodeTokens(Identifier nodeId, Identifier instanceId) {
    List<Token> nodeTokens = new ArrayList<Token>();
    for (final Iterator<Token> instanceTokens = getInstanceTokens(instanceId).getIterator(); instanceTokens.hasNext(); ) {
      Token token = instanceTokens.next();
      if(token.getNodeId().equals(nodeId)) {
        nodeTokens.add(token);
      }
    }
    return new TokenList(nodeTokens);
  }

  @Override
  public Token addToken(Identifier instanceId, Identifier nodeId, Option<Identifier> sourceNodeId) {
    Token token = new Token(idGenerator.generateId(),
      nodeId, sourceNodeId,
      Option.of(instanceId), false);

    getInstanceTokens(instanceId).add(token);
    return token;
  }

  @Override
  public boolean removeToken(Identifier instanceId, Identifier tokenIdToDelete) {
    for (final Iterator<Token> instanceTokens = getInstanceTokens(instanceId).getIterator(); instanceTokens.hasNext(); ) {
      Token nextToken = instanceTokens.next();
      if (nextToken.getId().equals(tokenIdToDelete)) {
        instanceTokens.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public Identifier createInstance(Identifier definitionId) {
    Identifier instanceId = idGenerator.generateId();
    getOrCreateInstanceList(definitionId).add(instanceId);
    return instanceId;
  }

  protected List<Identifier> getOrCreateInstanceList(Identifier flowId) {
    if (flowToInstanceMap.get(flowId) == null) {
      flowToInstanceMap.put(flowId, new ArrayList<Identifier>());
    }
    return flowToInstanceMap.get(flowId);
  }

}