# Архитектура Poker Game - Описание

## Обзор системы

Это консольная реализация Texas Hold'em Poker на Kotlin с использованием Gradle. Архитектура следует паттерну MVC с разделением на слои.

## Поток данных

1. **Запуск** (`Main.kt`): Создаются зависимости (Logger, Storage, View, Controller), запускается `controller.startGame()`
2. **Настройка игры**: Ввод количества игроков, имён, стартовых стеков, размеров блайндов
3. **Раздача**: `Controller.startHand()` запускает полный цикл игры:
   - Раздача карт (префлоп)
   - Блайнды (SB и BB)
   - Раунды ставок: префлоп → флоп → терн → ривер
   - Шоудан: оценка комбинаций, определение победителей, распределение банка
4. **Цикл**: После завершения раздачи спрашивается новая раздача или выход
5. **Сохранение/Загрузка**: Игра может быть сохранена в `.dat` файл и загружена по UUID

## Ключевые особенности

- **Side pots**: Поддержка боковых банков для all-in ситуаций
- **Тейкбрейк**: Корректное определение победителя при равных комбинациях
- **Многосторонний банк**: Распределение между несколькими победителями
- **Java serialization**: Сохранение состояния игры в бинарный формат

---

## Изменения в архитектуре

### Logger
- Теперь **интерфейс** с реализацией `ConsoleLogger`
- Методы: `info(msg)`, `error(msg)`

### Storage
- **Интерфейс** с реализацией `FileStorage`
- Использует **Java serialization** (ObjectOutputStream), не JSON
- Сохранение в `.dat` файлы по UUID

### Pot
- Добавлен внутренний **data class SidePot** с полями `eligiblePlayers` и `amount`
- Методы: `buildSidePots(players)`, `distributeWinners(winners)`, `getSidePots()`, `reset()`
- Поддержка боковых банков для all-in ситуаций

### Hand
- Поля: `endedEarly: Boolean`, `dealerIndex: Int`, `currentPlayerIndex: Int`
- Методы: `endHandEarly(winner)`, `isEndedEarly()`, `addCommunityCard(card)`, `getDealerIndex()`, `getCurrentPlayerIndex()`, `setCurrentPlayerIndex(index)`
- `phase: GamePhase` - публичное поле с приватным сеттером
- Методы `dealFlop()`, `dealTurn()`, `dealRiver()` сжигают карту перед раздачей

### Game
- Добавлено поле `currentBet: Int`
- Методы: `getCurrentHandOrThrow()`, `isHandInProgress()`, `getCurrentBet()`, `setCurrentBet(bet)`

### Controller
- Поля: `bigBlind: Int`, `smallBlind: Int`
- Публичные методы: `saveGame()`, `loadGame(id)`, `playerAction(player, action, amount)` (для GUI)
- Внутренние методы: `isGameEnded()`, `placeBlind()`, `findFirstActive()`, `runBettingRound()`, `handleShowdown()`

### Player
- Все поля private с геттерами/сеттерами
- `name: String` - публичное свойство
- Метод `bet(amount)` возвращает фактическую сумму ставки

### HandRank
- Поля `category` и `cards` - публичные свойства (val)
- Реализовано `Comparable<HandRank>` с корректным тейкбрейком

### HandEvaluator
- Метод `bestHand()` перебирает все C(7,5)=21 комбинацию из 7 карт
- Метод `detectCombination()` определяет тип комбинации по 5 картам
- Поддержка特殊ного стрита A-2-3-4-5 (старшая карта 5)

---

## Файловая структура

```
src/main/kotlin/
├── Enums.kt          // Suit, Rank, PlayerStatus, Action, GamePhase, Combination
├── Card.kt           // Карта (suit, rank)
├── Deck.kt           // Колода
├── Player.kt         // Игрок
├── Pot.kt            // Банк + SidePot
├── Hand.kt           // Раздача
├── Game.kt           // Игра (стол)
├── HandRank.kt       // Оценка руки
├── HandEvaluator.kt  // Оценка комбинаций
├── ActionProcessor.kt// Обработка действий
├── View.kt           // Интерфейс View
├── ViewCli.kt        // Консольная реализация
├── Controller.kt     // MVC контроллер
├── Logger.kt         // Интерфейс логирования
├── Storage.kt        // Интерфейс хранения
├── Main.kt           // Точка входа
```